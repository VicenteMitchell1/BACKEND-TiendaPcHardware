package cl.pchardware.boleta.service;

import cl.pchardware.boleta.client.DetalleClient;
import cl.pchardware.boleta.client.ProductoClient;
import cl.pchardware.boleta.client.ProductoClient.ItemStock;
import cl.pchardware.boleta.dto.BoletaRequest;
import cl.pchardware.boleta.dto.BoletaResponse;
import cl.pchardware.boleta.dto.BoletaUpdateRequest;
import cl.pchardware.boleta.dto.DetalleBoletaDTO;
import cl.pchardware.boleta.dto.UsuarioActual;
import cl.pchardware.boleta.exception.BadRequestException;
import cl.pchardware.boleta.exception.ConflictException;
import cl.pchardware.boleta.exception.ForbiddenException;
import cl.pchardware.boleta.exception.ServiceUnavailableException;
import cl.pchardware.boleta.exception.UnauthorizedException;
import cl.pchardware.boleta.model.Boleta;
import cl.pchardware.boleta.model.Cupon;
import cl.pchardware.boleta.repository.BoletaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class BoletaServiceTest {

    private static final UsuarioActual CLIENTE = new UsuarioActual(5L, "alumno@duoc.cl", "CLIENTE");
    private static final UsuarioActual OTRO_CLIENTE = new UsuarioActual(9L, "otro@duoc.cl", "CLIENTE");
    private static final UsuarioActual VENDEDOR = new UsuarioActual(3L, "vendedor@duoc.cl", "VENDEDOR");
    private static final UsuarioActual ADMIN = new UsuarioActual(1L, "admin@duoc.cl", "ADMIN");

    private BoletaRepository repo;
    private ProductoClient productoClient;
    private DetalleClient detalleClient;
    private CuponService cuponService;
    private BoletaService service;

    @BeforeEach
    void setUp() {
        repo = mock(BoletaRepository.class);
        productoClient = mock(ProductoClient.class);
        detalleClient = mock(DetalleClient.class);
        cuponService = mock(CuponService.class);
        service = new BoletaService(repo, productoClient, detalleClient, cuponService);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> {
            Boleta b = inv.getArgument(0);
            b.setId(100L);
            return b;
        });
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(detalleClient.saveBatch(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ProductoClient.ProductoDTO producto(long id, int precio) {
        ProductoClient.ProductoDTO p = new ProductoClient.ProductoDTO();
        p.setId(id);
        p.setSku("SKU-" + id);
        p.setNombre("Producto " + id);
        p.setPrecio(precio);
        return p;
    }

    private static BoletaRequest compra(long idProducto, int cantidad) {
        BoletaRequest r = new BoletaRequest();
        DetalleBoletaDTO item = new DetalleBoletaDTO();
        item.setIdProducto(idProducto);
        item.setCantidad(cantidad);
        r.setItems(List.of(item));
        return r;
    }

    private static Boleta boleta(long id, long idUsuario, String estado) {
        return Boleta.builder().id(id).numeroBoleta("BOL-" + id).subtotal(1000).total(1000).descuento(0)
                .estado(estado).idUsuario(idUsuario).build();
    }

    @Test
    @DisplayName("El total se calcula con el precio del catálogo: se ignora el total enviado por el cliente")
    void totalCalculadoEnServidor() {
        when(productoClient.descontarStock(anyList())).thenReturn(List.of(producto(1, 1_599_990)));
        BoletaRequest req = compra(1, 2);
        req.setTotal(1);
        req.setSubtotal(1);
        req.setDescuento(-5_000_000);

        BoletaResponse resp = service.create(req, CLIENTE);

        assertThat(resp.getTotal()).isEqualTo(3_199_980);
        assertThat(resp.getSubtotal()).isEqualTo(3_199_980);
        assertThat(resp.getDescuento()).isZero();
        assertThat(resp.getDetalles().get(0).getSubtotal()).isEqualTo(3_199_980);
    }

    @Test
    @DisplayName("El dueño de la boleta sale del token, no del cuerpo de la petición")
    void usuarioDesdeToken() {
        when(productoClient.descontarStock(anyList())).thenReturn(List.of(producto(1, 1000)));
        BoletaRequest req = compra(1, 1);
        req.setIdUsuario(4L);
        req.setEmailCliente("pancho@gmail.com");

        BoletaResponse resp = service.create(req, CLIENTE);

        assertThat(resp.getIdUsuario()).isEqualTo(5L);
        assertThat(resp.getEmailCliente()).isEqualTo("alumno@duoc.cl");
    }

    @Test
    @DisplayName("Si falla el guardado de detalles, se repone el stock y la compra falla (no queda boleta sin detalle)")
    void compensacionSiFallaDetalle() {
        when(productoClient.descontarStock(anyList())).thenReturn(List.of(producto(1, 1000)));
        when(detalleClient.saveBatch(anyList())).thenThrow(new ServiceUnavailableException("detalle caído"));

        assertThatThrownBy(() -> service.create(compra(1, 2), CLIENTE)).isInstanceOf(ServiceUnavailableException.class);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ItemStock>> captor = ArgumentCaptor.forClass(List.class);
        verify(productoClient).reponerStock(captor.capture());
        assertThat(captor.getValue()).containsExactly(new ItemStock(1L, 2));
    }

    @Test
    @DisplayName("Si no hay stock, no se crea la boleta ni se intenta compensar")
    void sinStock() {
        when(productoClient.descontarStock(anyList())).thenThrow(new BadRequestException("Stock insuficiente"));
        assertThatThrownBy(() -> service.create(compra(1, 50), CLIENTE)).isInstanceOf(BadRequestException.class);
        verify(repo, never()).saveAndFlush(any());
        verify(productoClient, never()).reponerStock(anyList());
    }

    @Test
    @DisplayName("Comprar sin usuario identificado -> 401")
    void compraSinUsuario() {
        assertThatThrownBy(() -> service.create(compra(1, 1), new UsuarioActual(null, null, null)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Un CLIENTE no puede ver boletas de otro cliente; sí las suyas")
    void propiedadCliente() {
        when(repo.findById(1L)).thenReturn(Optional.of(boleta(1, 5, "PAGADA")));
        assertThat(service.findById(1L, CLIENTE).getId()).isEqualTo(1L);
        assertThatThrownBy(() -> service.findById(1L, OTRO_CLIENTE)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("VENDEDOR y ADMIN pueden ver cualquier boleta")
    void personalVeTodo() {
        when(repo.findById(1L)).thenReturn(Optional.of(boleta(1, 5, "PAGADA")));
        assertThat(service.findById(1L, VENDEDOR)).isNotNull();
        assertThat(service.findById(1L, ADMIN)).isNotNull();
    }

    @Test
    @DisplayName("Un CLIENTE no puede listar todas las boletas")
    void listarTodasCliente() {
        assertThatThrownBy(() -> service.findAll(CLIENTE)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Listado de boletas pide los detalles en UNA sola llamada")
    void listarSinNmas1() {
        when(repo.findAllByOrderByFechaEmisionDesc()).thenReturn(List.of(boleta(1, 5, "PAGADA"), boleta(2, 6, "PAGADA")));
        when(detalleClient.findByBoletaIds(anyList())).thenReturn(Map.of());
        assertThat(service.findAll(VENDEDOR)).hasSize(2);
        verify(detalleClient, times(1)).findByBoletaIds(List.of(1L, 2L));
        verify(detalleClient, never()).findByBoletaId(any());
    }

    @Test
    @DisplayName("Anular una boleta repone el stock de sus productos")
    void anularRepone() {
        when(repo.findById(1L)).thenReturn(Optional.of(boleta(1, 5, "PAGADA")));
        when(detalleClient.findByBoletaId(1L)).thenReturn(List.of(
                DetalleBoletaDTO.builder().idProducto(3L).cantidad(2).build()));
        BoletaUpdateRequest req = new BoletaUpdateRequest();
        req.setEstado("ANULADA");

        assertThat(service.update(1L, req, ADMIN).getEstado()).isEqualTo("ANULADA");
        verify(productoClient).reponerStock(List.of(new ItemStock(3L, 2)));
    }

    @Test
    @DisplayName("Cupón válido: el descuento se calcula en el servidor sobre el subtotal del catálogo")
    void compraConCupon() {
        when(cuponService.obtenerParaCompra("hardware2026"))
                .thenReturn(Cupon.builder().codigo("HARDWARE2026").porcentaje(15).activo(true).build());
        when(productoClient.descontarStock(anyList())).thenReturn(List.of(producto(1, 1_599_990)));
        BoletaRequest req = compra(1, 2);
        req.setCodigoCupon("hardware2026");
        req.setDescuento(3_000_000); // un descuento enviado por el cliente se ignora

        BoletaResponse r = service.create(req, CLIENTE);

        assertThat(r.getSubtotal()).isEqualTo(3_199_980);
        assertThat(r.getDescuento()).isEqualTo(479_997);
        assertThat(r.getTotal()).isEqualTo(2_719_983);
        assertThat(r.getCodigoCupon()).isEqualTo("HARDWARE2026");
    }

    @Test
    @DisplayName("Cupón inválido -> 400 y el stock no se toca")
    void compraConCuponInvalido() {
        when(cuponService.obtenerParaCompra("FALSO")).thenThrow(new BadRequestException("Código de descuento no válido o vencido."));
        BoletaRequest req = compra(1, 1);
        req.setCodigoCupon("FALSO");

        assertThatThrownBy(() -> service.create(req, CLIENTE)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(productoClient, detalleClient);
    }

    @Test
    @DisplayName("Redondeo del descuento al peso más cercano")
    void redondeoDescuento() {
        assertThat(BoletaService.calcularDescuento(999, 15)).isEqualTo(150);   // 149,85
        assertThat(BoletaService.calcularDescuento(1000, 5)).isEqualTo(50);
        assertThat(BoletaService.calcularDescuento(0, 90)).isZero();
    }

    @Test
    @DisplayName("Una boleta anulada no puede reactivarse")
    void noReactivar() {
        when(repo.findById(1L)).thenReturn(Optional.of(boleta(1, 5, "ANULADA")));
        BoletaUpdateRequest req = new BoletaUpdateRequest();
        req.setEstado("PAGADA");
        assertThatThrownBy(() -> service.update(1L, req, ADMIN)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Sólo se eliminan boletas ANULADAS, y sólo un ADMIN")
    void eliminar() {
        when(repo.findById(1L)).thenReturn(Optional.of(boleta(1, 5, "PAGADA")));
        assertThatThrownBy(() -> service.delete(1L, ADMIN)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.delete(1L, VENDEDOR)).isInstanceOf(ForbiddenException.class);

        when(repo.findById(2L)).thenReturn(Optional.of(boleta(2, 5, "ANULADA")));
        service.delete(2L, ADMIN);
        verify(detalleClient).deleteByBoletaId(2L);
        verify(repo).delete(any(Boleta.class));
    }

    @Test
    @DisplayName("Comuna que no pertenece a la región -> 400 y el stock no se toca")
    void compraComunaDeOtraRegion() {
        BoletaRequest req = compra(1, 1);
        req.setRegion("Región de Valparaíso");
        req.setComuna("Santiago");

        assertThatThrownBy(() -> service.create(req, CLIENTE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no pertenece");
        verifyNoInteractions(productoClient, detalleClient);
    }

    @Test
    @DisplayName("Destino válido se guarda con el nombre oficial; sin destino se usa Santiago (RM)")
    void compraDestinoValido() {
        when(productoClient.descontarStock(anyList())).thenReturn(List.of(producto(1, 1000)));
        BoletaRequest req = compra(1, 1);
        req.setRegion(" región metropolitana de santiago ");
        req.setComuna("MAIPÚ");

        BoletaResponse r = service.create(req, CLIENTE);
        assertThat(r.getRegion()).isEqualTo("Región Metropolitana de Santiago");
        assertThat(r.getComuna()).isEqualTo("Maipú");

        BoletaResponse sinDestino = service.create(compra(1, 1), CLIENTE);
        assertThat(sinDestino.getRegion()).isEqualTo("Región Metropolitana de Santiago");
        assertThat(sinDestino.getComuna()).isEqualTo("Santiago");
    }

    @Test
    @DisplayName("Al editar una boleta, la comuna nueva debe pertenecer a la región (nueva o actual)")
    void editarDestino() {
        Boleta b = boleta(1, 5, "PAGADA");
        b.setRegion("Región Metropolitana de Santiago");
        b.setComuna("Providencia");
        when(repo.findById(1L)).thenReturn(Optional.of(b));

        BoletaUpdateRequest incoherente = new BoletaUpdateRequest();
        incoherente.setComuna("Viña del Mar");
        assertThatThrownBy(() -> service.update(1L, incoherente, ADMIN)).isInstanceOf(BadRequestException.class);
        assertThat(b.getComuna()).isEqualTo("Providencia");

        BoletaUpdateRequest cambio = new BoletaUpdateRequest();
        cambio.setRegion("Región de Valparaíso");
        cambio.setComuna("Viña del Mar");
        BoletaResponse r = service.update(1L, cambio, ADMIN);
        assertThat(r.getRegion()).isEqualTo("Región de Valparaíso");
        assertThat(r.getComuna()).isEqualTo("Viña del Mar");
    }
}
