package cl.pchardware.boleta.service;

import cl.pchardware.boleta.dto.CuponRequest;
import cl.pchardware.boleta.dto.UsuarioActual;
import cl.pchardware.boleta.exception.BadRequestException;
import cl.pchardware.boleta.exception.ConflictException;
import cl.pchardware.boleta.exception.ForbiddenException;
import cl.pchardware.boleta.exception.ResourceNotFoundException;
import cl.pchardware.boleta.model.Cupon;
import cl.pchardware.boleta.repository.CuponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CuponServiceTest {

    private static final UsuarioActual ADMIN = new UsuarioActual(1L, "admin@duoc.cl", "ADMIN");
    private static final UsuarioActual VENDEDOR = new UsuarioActual(3L, "vendedor@duoc.cl", "VENDEDOR");

    private CuponRepository repo;
    private CuponService service;

    @BeforeEach
    void setUp() {
        repo = mock(CuponRepository.class);
        service = new CuponService(repo);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Cupon cupon(String codigo, int pct, boolean activo) {
        return Cupon.builder().id(1L).codigo(codigo).porcentaje(pct).activo(activo).build();
    }

    @Test
    @DisplayName("Validar normaliza el código (espacios y minúsculas) y exige que esté activo")
    void validar() {
        when(repo.findByCodigo("GAMER10")).thenReturn(Optional.of(cupon("GAMER10", 10, true)));
        when(repo.findByCodigo("VIEJO")).thenReturn(Optional.of(cupon("VIEJO", 10, false)));

        assertThat(service.validar("  gamer10 ").porcentaje()).isEqualTo(10);
        assertThatThrownBy(() -> service.validar("VIEJO")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.validar("NADA")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.obtenerParaCompra("VIEJO")).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Crear guarda el código en mayúsculas y rechaza duplicados con 409")
    void crear() {
        CuponRequest req = CuponRequest.builder().codigo("verano20").porcentaje(20).build();
        assertThat(service.create(req, ADMIN).codigo()).isEqualTo("VERANO20");

        when(repo.existsByCodigo("VERANO20")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req, ADMIN)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Editar a un código ya usado por otro cupón -> 409")
    void editarDuplicado() {
        when(repo.findById(1L)).thenReturn(Optional.of(cupon("A1", 5, true)));
        when(repo.existsByCodigoAndIdNot("GAMER10", 1L)).thenReturn(true);
        CuponRequest req = CuponRequest.builder().codigo("gamer10").porcentaje(5).build();
        assertThatThrownBy(() -> service.update(1L, req, ADMIN)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Sólo ADMIN gestiona cupones (defensa en profundidad, además del gateway)")
    void soloAdmin() {
        CuponRequest req = CuponRequest.builder().codigo("X1X").porcentaje(5).build();
        assertThatThrownBy(() -> service.findAll(VENDEDOR)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.create(req, VENDEDOR)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.delete(1L, VENDEDOR)).isInstanceOf(ForbiddenException.class);
        verify(repo, never()).save(any());
    }
}
