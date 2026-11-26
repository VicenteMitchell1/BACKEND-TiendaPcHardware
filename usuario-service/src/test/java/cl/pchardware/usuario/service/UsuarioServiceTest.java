package cl.pchardware.usuario.service;

import cl.pchardware.usuario.dto.CredencialesRequest;
import cl.pchardware.usuario.dto.RegisterRequest;
import cl.pchardware.usuario.dto.CambioPasswordRequest;
import cl.pchardware.usuario.dto.PerfilRequest;
import cl.pchardware.usuario.dto.UsuarioActual;
import cl.pchardware.usuario.dto.UsuarioInternalDTO;
import cl.pchardware.usuario.model.AccionHistorial;
import cl.pchardware.usuario.model.HistorialUsuario;
import cl.pchardware.usuario.dto.UsuarioRequest;
import cl.pchardware.usuario.exception.BadRequestException;
import cl.pchardware.usuario.exception.ConflictException;
import cl.pchardware.usuario.exception.ForbiddenException;
import cl.pchardware.usuario.exception.UnauthorizedException;
import cl.pchardware.usuario.model.Rol;
import cl.pchardware.usuario.model.Usuario;
import cl.pchardware.usuario.repository.HistorialUsuarioRepository;
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
    /** Administrador que hace los cambios (id distinto al de las cuentas de prueba, que usan id 1). */
    private static final UsuarioActual ADMIN = new UsuarioActual(50L, "jefe@duoc.cl", "ADMIN");
    private HistorialUsuarioRepository historialRepo;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        repo = mock(UsuarioRepository.class);
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            if (u.getId() == null) u.setId(99L);
            return u;
        });
        historialRepo = mock(HistorialUsuarioRepository.class);
        service = new UsuarioService(repo, historialRepo, encoder);
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
        assertThatThrownBy(() -> service.create(req, ADMIN)).isInstanceOf(BadRequestException.class);
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
        assertThatThrownBy(() -> service.create(req, ADMIN)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Rol inválido -> 400 (antes se ignoraba en silencio)")
    void crearRolInvalido() {
        UsuarioRequest req = new UsuarioRequest();
        req.setEmail("n@duoc.cl");
        req.setPassword("clave");
        req.setNombre("N");
        req.setRol("SUPERADMIN");
        assertThatThrownBy(() -> service.create(req, ADMIN)).isInstanceOf(BadRequestException.class).hasMessageContaining("Rol inválido");
    }

    @Test
    @DisplayName("Crear usuario guarda la contraseña con BCrypt, nunca en texto plano")
    void crearGuardaHash() {
        UsuarioRequest req = new UsuarioRequest();
        req.setEmail("v2@duoc.cl");
        req.setPassword("secreta");
        req.setNombre("Vendedora");
        req.setRol("vendedor");

        service.create(req, ADMIN);

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
        assertThatThrownBy(() -> service.update(1L, req, ADMIN)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Desactivar un usuario desde la edición")
    void actualizarDesactiva() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario("a@duoc.cl", "x", Rol.CLIENTE, true)));
        UsuarioRequest req = new UsuarioRequest();
        req.setEmail("a@duoc.cl");
        req.setNombre("A");
        req.setActivo(false);
        assertThat(service.update(1L, req, ADMIN).getActivo()).isFalse();
    }

    private HistorialUsuario ultimoHistorial() {
        ArgumentCaptor<HistorialUsuario> captor = ArgumentCaptor.forClass(HistorialUsuario.class);
        verify(historialRepo, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("El ADMIN no puede cambiar la contraseña de otro usuario -> 400 y no se guarda nada")
    void adminNoCambiaClave() {
        UsuarioRequest req = new UsuarioRequest();
        req.setEmail("a@duoc.cl");
        req.setNombre("A");
        req.setPassword("nueva123");
        assertThatThrownBy(() -> service.update(1L, req, ADMIN))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("contraseña");
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("El ADMIN edita datos y rol de otro usuario; el historial guarda cada cambio y quién lo hizo")
    void adminEditaConHistorial() {
        Usuario u = usuario("a@duoc.cl", "x", Rol.CLIENTE, true);
        u.setNombre("Ana");
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        UsuarioRequest req = new UsuarioRequest();
        req.setEmail("ana@duoc.cl");
        req.setNombre("Ana María");
        req.setRol("VENDEDOR");

        assertThat(service.update(1L, req, ADMIN).getRol()).isEqualTo("VENDEDOR");

        HistorialUsuario h = ultimoHistorial();
        assertThat(h.getAccion()).isEqualTo(AccionHistorial.EDICION);
        assertThat(h.getIdUsuario()).isEqualTo(1L);
        assertThat(h.getEmailUsuario()).isEqualTo("a@duoc.cl");
        assertThat(h.getEmailAutor()).isEqualTo("jefe@duoc.cl");
        assertThat(h.getDetalle()).contains("Email: 'a@duoc.cl' → 'ana@duoc.cl'", "Nombre: 'Ana' → 'Ana María'",
                "Rol: 'CLIENTE' → 'VENDEDOR'");
    }

    @Test
    @DisplayName("Nadie puede cambiar el RUT (ni el ADMIN); reenviar el mismo RUT sí se acepta")
    void rutInmutable() {
        Usuario u = usuario("a@duoc.cl", "x", Rol.CLIENTE, true);
        u.setRut("18555444-3");
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        UsuarioRequest cambio = new UsuarioRequest();
        cambio.setEmail("a@duoc.cl");
        cambio.setNombre("A");
        cambio.setRut("11111111-1");
        assertThatThrownBy(() -> service.update(1L, cambio, ADMIN))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("RUT");
        verify(repo, never()).save(any());

        UsuarioRequest mismo = new UsuarioRequest();
        mismo.setEmail("a@duoc.cl");
        mismo.setNombre("A");
        mismo.setRut(" 18555444-3 ");
        mismo.setActivo(false);
        assertThat(service.update(1L, mismo, ADMIN).getRut()).isEqualTo("18555444-3");
    }

    @Test
    @DisplayName("El ADMIN no puede quitarse su rol ni desactivar su propia cuenta")
    void adminNoSeQuitaRol() {
        Usuario propio = usuario("jefe@duoc.cl", "x", Rol.ADMIN, true);
        propio.setId(50L);
        when(repo.findById(50L)).thenReturn(Optional.of(propio));
        UsuarioRequest bajarRol = new UsuarioRequest();
        bajarRol.setEmail("jefe@duoc.cl");
        bajarRol.setNombre("Jefe");
        bajarRol.setRol("CLIENTE");
        assertThatThrownBy(() -> service.update(50L, bajarRol, ADMIN)).isInstanceOf(BadRequestException.class);

        UsuarioRequest desactivar = new UsuarioRequest();
        desactivar.setEmail("jefe@duoc.cl");
        desactivar.setNombre("Jefe");
        desactivar.setActivo(false);
        assertThatThrownBy(() -> service.update(50L, desactivar, ADMIN)).isInstanceOf(BadRequestException.class);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Mi perfil: el usuario cambia nombre, apellido y email, pero no su rol")
    void actualizarPerfilPropio() {
        Usuario u = usuario("alumno@duoc.cl", "1234", Rol.CLIENTE, true);
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        PerfilRequest req = new PerfilRequest();
        req.setEmail("Alumno.Nuevo@duoc.cl");
        req.setNombre("Alumno");
        req.setApellido("Duoc");

        var resp = service.actualizarPerfil(new UsuarioActual(1L, "alumno@duoc.cl", "CLIENTE"), req);

        assertThat(resp.getEmail()).isEqualTo("alumno.nuevo@duoc.cl");
        assertThat(resp.getRol()).isEqualTo("CLIENTE");
        assertThat(ultimoHistorial().getEmailAutor()).isEqualTo("alumno@duoc.cl");
    }

    @Test
    @DisplayName("Cambio de contraseña: exige la actual, guarda el hash nuevo y el historial no incluye claves")
    void cambiarPasswordPropia() {
        Usuario u = usuario("alumno@duoc.cl", "1234", Rol.CLIENTE, true);
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        UsuarioActual yo = new UsuarioActual(1L, "alumno@duoc.cl", "CLIENTE");

        assertThatThrownBy(() -> service.cambiarPassword(yo, new CambioPasswordRequest("otra", "nueva123")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("actual");
        assertThatThrownBy(() -> service.cambiarPassword(yo, new CambioPasswordRequest("1234", "1234")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("distinta");

        service.cambiarPassword(yo, new CambioPasswordRequest("1234", "nueva123"));

        assertThat(encoder.matches("nueva123", u.getPasswordHash())).isTrue();
        HistorialUsuario h = ultimoHistorial();
        assertThat(h.getAccion()).isEqualTo(AccionHistorial.CAMBIO_CONTRASENA);
        assertThat(h.getDetalle()).doesNotContain("1234", "nueva123");
    }

    @Test
    @DisplayName("Sin sesión no se puede consultar ni editar el perfil -> 401")
    void perfilSinSesion() {
        assertThatThrownBy(() -> service.perfil(UsuarioActual.ANONIMO)).isInstanceOf(UnauthorizedException.class);
    }
}
