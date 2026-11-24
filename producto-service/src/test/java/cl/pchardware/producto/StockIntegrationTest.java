package cl.pchardware.producto;

import cl.pchardware.producto.client.CategoriaClient;
import cl.pchardware.producto.dto.MovimientoStockRequest;
import cl.pchardware.producto.exception.BadRequestException;
import cl.pchardware.producto.model.Producto;
import cl.pchardware.producto.repository.ProductoRepository;
import cl.pchardware.producto.service.ProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integración real con la base H2: atomicidad y concurrencia del descuento de stock. */
@SpringBootTest
@AutoConfigureMockMvc
class StockIntegrationTest {

    private static final String KEY = "X-Internal-Token";
    private static final String VALOR = "test-internal-key";

    @Autowired private ProductoService service;
    @Autowired private ProductoRepository repo;
    @Autowired private MockMvc mvc;
    @MockBean private CategoriaClient categoriaClient;

    private Producto nuevo(int stock) {
        return repo.save(Producto.builder().sku("T-" + UUID.randomUUID()).nombre("Prueba").precio(1000)
                .stock(stock).activo(true).esOferta(false).idCategoria(1L).nombreCategoria("Test").build());
    }

    private static MovimientoStockRequest compra(Object... idCantidad) {
        List<MovimientoStockRequest.Item> items = new java.util.ArrayList<>();
        for (int i = 0; i < idCantidad.length; i += 2) {
            items.add(new MovimientoStockRequest.Item((Long) idCantidad[i], (Integer) idCantidad[i + 1]));
        }
        return new MovimientoStockRequest(items);
    }

    @Test
    @DisplayName("Compra con un item sin stock: NO se descuenta ningún item (todo o nada)")
    void atomicidad() {
        Producto a = nuevo(5);
        Producto b = nuevo(1);

        assertThatThrownBy(() -> service.descontarStock(compra(a.getId(), 2, b.getId(), 3)))
                .isInstanceOf(BadRequestException.class);

        assertThat(repo.findStockById(a.getId())).contains(5);
        assertThat(repo.findStockById(b.getId())).contains(1);
    }

    @Test
    @DisplayName("El mismo producto repetido en la compra se valida contra el stock acumulado")
    void productoRepetido() {
        Producto a = nuevo(2);
        assertThatThrownBy(() -> service.descontarStock(compra(a.getId(), 2, a.getId(), 1)))
                .isInstanceOf(BadRequestException.class);
        assertThat(repo.findStockById(a.getId())).contains(2);
    }

    @Test
    @DisplayName("20 compras simultáneas de 1 unidad con stock 10: exactamente 10 éxitos y stock final 0")
    void concurrencia() throws Exception {
        Producto p = nuevo(10);
        int hilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();

        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                largada.await();
                try {
                    service.descontarStock(compra(p.getId(), 1));
                    exitos.incrementAndGet();
                } catch (BadRequestException e) {
                    rechazos.incrementAndGet();
                }
                return null;
            });
        }
        largada.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(exitos.get()).isEqualTo(10);
        assertThat(rechazos.get()).isEqualTo(10);
        assertThat(repo.findStockById(p.getId())).contains(0);
    }

    @Test
    @DisplayName("Reponer devuelve las unidades al inventario")
    void reponer() {
        Producto p = nuevo(3);
        service.descontarStock(compra(p.getId(), 3));
        service.reponerStock(compra(p.getId(), 3));
        assertThat(repo.findStockById(p.getId())).contains(3);
    }

    @Test
    @DisplayName("Endpoint de stock: sin clave 401, cantidad nula 400, lote vacío 400")
    void endpointsStock() throws Exception {
        mvc.perform(post("/productos/1/stock").contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/productos/1/stock").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.cantidad").exists());
        mvc.perform(post("/productos/stock/descontar").header(KEY, VALOR).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Conteo de productos por categoría (usado para no borrar categorías con productos)")
    void contarPorCategoria() throws Exception {
        mvc.perform(get("/productos/categoria/5/total").header(KEY, VALOR))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").isNumber());
    }
}
