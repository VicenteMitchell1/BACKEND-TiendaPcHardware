package cl.pchardware.producto.service;

import cl.pchardware.producto.client.CategoriaClient;
import cl.pchardware.producto.dto.MovimientoStockRequest;
import cl.pchardware.producto.dto.ProductoRequest;
import cl.pchardware.producto.exception.BadRequestException;
import cl.pchardware.producto.exception.ConflictException;
import cl.pchardware.producto.exception.ResourceNotFoundException;
import cl.pchardware.producto.model.Producto;
import cl.pchardware.producto.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class ProductoServiceTest {

    private ProductoRepository repo;
    private CategoriaClient categoriaClient;
    private ProductoService service;

    @BeforeEach
    void setUp() {
        repo = mock(ProductoRepository.class);
        categoriaClient = mock(CategoriaClient.class);
        service = new ProductoService(repo, categoriaClient);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ProductoRequest req(String sku, long idCategoria) {
        ProductoRequest r = new ProductoRequest();
        r.setSku(sku);
        r.setNombre("Producto " + sku);
        r.setPrecio(1000);
        r.setStock(5);
        r.setIdCategoria(idCategoria);
        return r;
    }

    @Test
    @DisplayName("Crear producto toma el nombre de la categoría desde categoria-service")
    void crearConCategoriaValida() {
        when(categoriaClient.obtenerNombre(2L)).thenReturn("Procesadores");
        assertThat(service.create(req("CPU-1", 2L)).getNombreCategoria()).isEqualTo("Procesadores");
    }

    @Test
    @DisplayName("Crear producto con categoría inexistente -> 400 y no se guarda")
    void crearCategoriaInexistente() {
        when(categoriaClient.obtenerNombre(9999L)).thenThrow(new BadRequestException("La categoría con ID 9999 no existe."));
        assertThatThrownBy(() -> service.create(req("X-1", 9999L))).isInstanceOf(BadRequestException.class);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("SKU duplicado al crear o al editar -> 409")
    void skuDuplicado() {
        when(repo.existsBySku("GPU-1")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req("GPU-1", 1L))).isInstanceOf(ConflictException.class);

        when(repo.findById(5L)).thenReturn(Optional.of(Producto.builder().id(5L).sku("OTRO").build()));
        when(repo.existsBySkuAndIdNot("GPU-1", 5L)).thenReturn(true);
        assertThatThrownBy(() -> service.update(5L, req("GPU-1", 1L))).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("No se puede comprar un producto inactivo")
    void descontarInactivo() {
        when(repo.findById(1L)).thenReturn(Optional.of(Producto.builder().id(1L).nombre("Viejo").stock(10).activo(false).build()));
        MovimientoStockRequest r = new MovimientoStockRequest(List.of(new MovimientoStockRequest.Item(1L, 1)));
        assertThatThrownBy(() -> service.descontarStock(r)).isInstanceOf(BadRequestException.class).hasMessageContaining("no está disponible");
        verify(repo, never()).descontarStock(any(), anyInt());
    }

    @Test
    @DisplayName("Stock insuficiente informa cuántas unidades quedan")
    void descontarSinStock() {
        when(repo.findById(1L)).thenReturn(Optional.of(Producto.builder().id(1L).nombre("RTX").stock(2).activo(true).build()));
        when(repo.descontarStock(1L, 5)).thenReturn(0);
        when(repo.findStockById(1L)).thenReturn(Optional.of(2));
        MovimientoStockRequest r = new MovimientoStockRequest(List.of(new MovimientoStockRequest.Item(1L, 5)));
        assertThatThrownBy(() -> service.descontarStock(r)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Disponibles: 2, solicitados: 5");
    }

    @Test
    @DisplayName("Reponer stock de un producto inexistente -> 404")
    void reponerInexistente() {
        when(repo.reponerStock(77L, 1)).thenReturn(0);
        MovimientoStockRequest r = new MovimientoStockRequest(List.of(new MovimientoStockRequest.Item(77L, 1)));
        assertThatThrownBy(() -> service.reponerStock(r)).isInstanceOf(ResourceNotFoundException.class);
    }
}
