package cl.pchardware.usuario.service;

import cl.pchardware.usuario.dto.CredencialesRequest;
import cl.pchardware.usuario.dto.RegisterRequest;
import cl.pchardware.usuario.dto.UsuarioInternalDTO;
import cl.pchardware.usuario.dto.UsuarioRequest;
import cl.pchardware.usuario.exception.BadRequestException;
import cl.pchardware.usuario.exception.ConflictException;
import cl.pchardware.usuario.exception.ForbiddenException;
import cl.pchardware.usuario.exception.UnauthorizedException;
import cl.pchardware.usuario.model.Rol;
import cl.pchardware.usuario.model.Usuario;
import cl.pchardware.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // costo bajo: tests rápidos
    private UsuarioRepository repo;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        repo = mock(UsuarioRepository.class);
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            if (u.getId() == null) u.setId(99L);
            return u;
        });
        service = new UsuarioService(repo, encoder);
    }

    private Usuario usuario(String email, String clave, Rol rol, boolean activo) {
        return Usuario.builder().id(1L).email(email).passwordHash(encoder.encode(clave))
                .nombre("Test").rol(rol).activo(activo).build();
    }

    @Test
    @DisplayName("Credenciales válidas devuelven los datos de sesión sin hash de contraseña")
    void verificarCredencialesValidas() {
        when(repo.findByEmail("admin@duoc.cl")).thenReturn(Optional.of(usuario("admin@duoc.cl", "admin", Rol.ADMIN, true)));

        UsuarioInternalDTO dto = service.verificarCredenciales(new CredencialesRequest("  ADMIN@duoc.cl ", "admin"));

        assertThat(dto.getEmail()).isEqualTo("admin@duoc.cl");
        assertThat(dto.getRol()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Contraseña incorrecta -> 401")
    void verificarClaveIncorrecta() {
        when(repo.findByEmail("admin@duoc.cl")).thenReturn(Optional.of(usuario("admin@duoc.cl", "admin", Rol.ADMIN, true)));
        assertThatThrownBy(() -> service.verificarCredenciales(new CredencialesRequest("admin@duoc.cl", "otra")))
                .isInstanceOf(UnauthorizedException.class).hasMessage("Credenciales incorrectas.");
    }

    @Test
    @DisplayName("Email inexistente -> 401 con el mismo mensaje (no revela si el usuario existe)")
    void verificarEmailInexistente() {
        when(repo.findByEmail(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verificarCredenciales(new CredencialesRequest("nadie@x.cl", "x")))
                .isInstanceOf(UnauthorizedException.class).hasMessage("Credenciales incorrectas.");
    }

    @Test
    @DisplayName("Cuenta desactivada -> 403 aunque la contraseña sea correcta")
    void verificarCuentaDesactivada() {
        when(repo.findByEmail("x@duoc.cl")).thenReturn(Optional.of(usuario("x@duoc.cl", "1234", Rol.CLIENTE, false)));
        assertThatThrownBy(() -> service.verificarCredenciales(new CredencialesRequest("x@duoc.cl", "1234")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Crear usuario sin contraseña -> 400")
    void crearSinClave() {
        UsuarioRequest req = new UsuarioRequest();
        req.setEmail("nuevo@duoc.cl");
        req.setNombre("Nuevo");
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(BadRequestException.class);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Crear usuario con email existente -> 409")
    void crearEmailDuplicado() {
        when(repo.existsByEmail("admin@duoc.cl")).thenReturn(true);
        UsuarioRequest req = new UsuarioRequest();
        req.setEmail("Admin@Duoc.cl");
        req.setPassword("clave");
        req.setNombre("X");
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Rol inválido -> 400 (antes se ignoraba en silencio)")
    void crearRolInvalido() {
        UsuarioRequest req = new UsuarioRequest();
        req.setEmail("n@duoc.cl");
        req.setPassword("clave");
        req.setNombre("N");
        req.setRol("SUPERADMIN");
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(BadRequestException.class).hasMessageContaining("Rol inválido");
    }

    @Test
    @DisplayName("Crear usuario guarda la contraseña con BCrypt, nunca en texto plano")
    void crearGuardaHash() {
        UsuarioRequest req = new UsuarioRequest();
        req.setEmail("v2@duoc.cl");
        req.setPassword("secreta");
        req.setNombre("Vendedora");
        req.setRol("vendedor");

        service.create(req);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("secreta").startsWith("$2a$");
        assertThat(encoder.matches("secreta", captor.getValue().getPasswordHash())).isTrue();
        assertThat(captor.getValue().getRol()).isEqualTo(Rol.VENDEDOR);
    }

    @Test
    @DisplayName("El registro público siempre asigna rol CLIENTE")
    void registroSiempreCliente() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("cliente@test.cl");
        req.setPassword("1234");
        req.setNombre("Cliente");

        UsuarioInternalDTO dto = service.register(req);

        assertThat(dto.getRol()).isEqualTo("CLIENTE");
        assertThat(dto.getActivo()).isTrue();
    }

    @Test
    @DisplayName("Cambiar el email a uno ya usado por otro usuario -> 409")
    void actualizarEmailDuplicado() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario("a@duoc.cl", "x", Rol.CLIENTE, true)));
        when(repo.existsByEmail("b@duoc.cl")).thenReturn(true);
        UsuarioRequest req = new UsuarioRequest();
        req.setEmail("b@duoc.cl");
        req.setNombre("A");
        assertThatThrownBy(() -> service.update(1L, req)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Desactivar un usuario desde la edición")
    void actualizarDesactiva() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario("a@duoc.cl", "x", Rol.CLIENTE, true)));
        UsuarioRequest req = new UsuarioRequest();
        req.setEmail("a@duoc.cl");
        req.setNombre("A");
        req.setActivo(false);
        assertThat(service.update(1L, req).getActivo()).isFalse();
    }
}
