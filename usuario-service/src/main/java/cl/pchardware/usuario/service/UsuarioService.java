package cl.pchardware.usuario.service;

import cl.pchardware.usuario.dto.*;
import cl.pchardware.usuario.exception.BadRequestException;
import cl.pchardware.usuario.exception.ConflictException;
import cl.pchardware.usuario.exception.ForbiddenException;
import cl.pchardware.usuario.exception.ResourceNotFoundException;
import cl.pchardware.usuario.exception.UnauthorizedException;
import cl.pchardware.usuario.model.AccionHistorial;
import cl.pchardware.usuario.model.HistorialUsuario;
import cl.pchardware.usuario.model.Rol;
import cl.pchardware.usuario.model.Usuario;
import cl.pchardware.usuario.repository.HistorialUsuarioRepository;
import cl.pchardware.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final HistorialUsuarioRepository historialRepo;
    private final PasswordEncoder encoder;
    /** Hash usado cuando el email no existe, para que la respuesta tarde lo mismo (evita enumerar usuarios). */
    private final String hashFicticio;

    public UsuarioService(UsuarioRepository repo, HistorialUsuarioRepository historialRepo, PasswordEncoder encoder) {
        this.repo = repo;
        this.historialRepo = historialRepo;
        this.encoder = encoder;
        this.hashFicticio = encoder.encode("hash-ficticio-para-tiempo-constante");
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse findById(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional(readOnly = true)
    public UsuarioInternalDTO findInternalById(Long id) {
        return toInternal(buscar(id));
    }

    @Transactional(readOnly = true)
    public UsuarioInternalDTO findInternalByEmail(String email) {
        return toInternal(repo.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email)));
    }

    /**
     * Verifica email + contraseña dentro de este servicio: el hash BCrypt nunca sale de usuario-service.
     * Devuelve 401 con el mismo mensaje si el email no existe o la clave no coincide, y 403 si la cuenta está desactivada.
     */
    @Transactional(readOnly = true)
    public UsuarioInternalDTO verificarCredenciales(CredencialesRequest req) {
        Optional<Usuario> usuario = repo.findByEmail(normalizarEmail(req.getEmail()));
        String hash = usuario.map(Usuario::getPasswordHash).orElse(hashFicticio);
        boolean claveCorrecta = encoder.matches(req.getPassword(), hash);
        if (usuario.isEmpty() || !claveCorrecta) {
            throw new UnauthorizedException("Credenciales incorrectas.");
        }
        if (!Boolean.TRUE.equals(usuario.get().getActivo())) {
            throw new ForbiddenException("La cuenta está desactivada. Contacte al administrador.");
        }
        return toInternal(usuario.get());
    }

    // ------------------------------------------------------------------ administración (ADMIN)

    @Transactional
    public UsuarioResponse create(UsuarioRequest req, UsuarioActual autor) {
        if (!StringUtils.hasText(req.getPassword())) {
            throw new BadRequestException("La contraseña es obligatoria al crear un usuario.");
        }
        String email = normalizarEmail(req.getEmail());
        validarEmailDisponible(email);
        Usuario u = repo.save(Usuario.builder()
                .email(email)
                .passwordHash(encoder.encode(req.getPassword()))
                .nombre(req.getNombre().trim())
                .apellido(textoOVacio(req.getApellido()))
                .rut(textoOVacio(req.getRut()))
                .rol(StringUtils.hasText(req.getRol()) ? parseRol(req.getRol()) : Rol.CLIENTE)
                .activo(req.getActivo() == null || req.getActivo())
                .build());
        registrar(u, autor, AccionHistorial.CREACION, "Rol: " + u.getRol() + (u.getActivo() ? "" : " (inactivo)"));
        return toResponse(u);
    }

    /** Registro público desde la tienda: el rol siempre es CLIENTE, sin importar lo que envíe el cliente. */
    @Transactional
    public UsuarioInternalDTO register(RegisterRequest req) {
        String email = normalizarEmail(req.getEmail());
        validarEmailDisponible(email);
        Usuario u = repo.save(Usuario.builder()
                .email(email)
                .passwordHash(encoder.encode(req.getPassword()))
                .nombre(req.getNombre().trim())
                .apellido(textoOVacio(req.getApellido()))
                .rut(textoOVacio(req.getRut()))
                .rol(Rol.CLIENTE).activo(true)
                .build());
        registrar(u, new UsuarioActual(u.getId(), u.getEmail(), u.getRol().name()), AccionHistorial.REGISTRO,
                "Registro desde la tienda");
        return toInternal(u);
    }

    /**
     * Edición por el ADMIN: puede cambiar todos los datos de cualquier cuenta salvo la contraseña,
     * que sólo cambia su dueño (con la contraseña actual), y el RUT, que no cambia nadie. Sobre su propia cuenta no puede quitarse
     * el rol ADMIN ni desactivarse, para que el sistema no quede sin administrador.
     */
    @Transactional
    public UsuarioResponse update(Long id, UsuarioRequest req, UsuarioActual autor) {
        if (StringUtils.hasText(req.getPassword())) {
            throw new BadRequestException("El administrador no puede cambiar la contraseña de un usuario: "
                    + "sólo su dueño puede hacerlo desde Mi perfil.");
        }
        Usuario u = buscar(id);
        boolean esPropia = autor.identificado() && autor.id().equals(u.getId());
        Rol rolNuevo = StringUtils.hasText(req.getRol()) ? parseRol(req.getRol()) : u.getRol();
        if (esPropia && rolNuevo != Rol.ADMIN) {
            throw new BadRequestException("No puede quitarse el rol de administrador a sí mismo.");
        }
        if (esPropia && Boolean.FALSE.equals(req.getActivo())) {
            throw new BadRequestException("No puede desactivar la cuenta con la que inició sesión.");
        }
        // El RUT identifica a la persona: se fija al crear la cuenta y nadie lo cambia, ni siquiera el ADMIN.
        // Se acepta que venga el mismo valor, porque el frontend reenvía el registro completo.
        if (req.getRut() != null && !req.getRut().trim().equals(textoOVacio(u.getRut()))) {
            throw new BadRequestException("El RUT no se puede modificar.");
        }

        Cambios cambios = new Cambios();
        String emailAnterior = u.getEmail();
        aplicarEmail(u, req.getEmail(), cambios);
        cambios.texto("Nombre", u.getNombre(), req.getNombre().trim());
        u.setNombre(req.getNombre().trim());
        if (req.getApellido() != null) {
            cambios.texto("Apellido", u.getApellido(), req.getApellido().trim());
            u.setApellido(req.getApellido().trim());
        }
        cambios.texto("Rol", u.getRol().name(), rolNuevo.name());
        u.setRol(rolNuevo);
        if (req.getActivo() != null) {
            cambios.texto("Estado", estado(u.getActivo()), estado(req.getActivo()));
            u.setActivo(req.getActivo());
        }
        Usuario guardado = repo.save(u);
        if (cambios.hay()) registrar(guardado, emailAnterior, autor, AccionHistorial.EDICION, cambios.toString());
        return toResponse(guardado);
    }

    @Transactional
    public void delete(Long id, UsuarioActual autor) {
        Usuario u = buscar(id);
        if (autor.identificado() && autor.id().equals(u.getId())) {
            throw new BadRequestException("No puede eliminar la cuenta con la que inició sesión.");
        }
        repo.delete(u);
        registrar(u, autor, AccionHistorial.ELIMINACION,
                "Cuenta " + u.getRol() + " de " + nombreCompleto(u) + " eliminada");
    }

    @Transactional(readOnly = true)
    public List<HistorialResponse> historial(Long idUsuario) {
        List<HistorialUsuario> filas = idUsuario == null
                ? historialRepo.findTop200ByOrderByFechaDescIdDesc()
                : historialRepo.findByIdUsuarioOrderByFechaDescIdDesc(idUsuario);
        return filas.stream().map(UsuarioService::toResponse).toList();
    }

    // ------------------------------------------------------------------ Mi perfil (cualquier usuario con sesión)

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(UsuarioActual actual) {
        return toResponse(buscarPropio(actual));
    }

    /** El usuario cambia su nombre, apellido y email. El rol, el estado y el RUT no se editan aquí. */
    @Transactional
    public UsuarioResponse actualizarPerfil(UsuarioActual actual, PerfilRequest req) {
        Usuario u = buscarPropio(actual);
        Cambios cambios = new Cambios();
        String emailAnterior = u.getEmail();
        aplicarEmail(u, req.getEmail(), cambios);
        cambios.texto("Nombre", u.getNombre(), req.getNombre().trim());
        u.setNombre(req.getNombre().trim());
        cambios.texto("Apellido", u.getApellido(), textoOVacio(req.getApellido()));
        u.setApellido(textoOVacio(req.getApellido()));
        Usuario guardado = repo.save(u);
        if (cambios.hay()) registrar(guardado, emailAnterior, actual, AccionHistorial.EDICION, cambios.toString());
        return toResponse(guardado);
    }

    /** Cambio de contraseña por su dueño: exige la contraseña actual. Responde 400 si no coincide. */
    @Transactional
    public void cambiarPassword(UsuarioActual actual, CambioPasswordRequest req) {
        Usuario u = buscarPropio(actual);
        if (!encoder.matches(req.getPasswordActual(), u.getPasswordHash())) {
            throw new BadRequestException("La contraseña actual no es correcta.");
        }
        if (encoder.matches(req.getPasswordNueva(), u.getPasswordHash())) {
            throw new BadRequestException("La contraseña nueva debe ser distinta de la actual.");
        }
        u.setPasswordHash(encoder.encode(req.getPasswordNueva()));
        repo.save(u);
        registrar(u, actual, AccionHistorial.CAMBIO_CONTRASENA, "El usuario cambió su contraseña");
    }

    // ------------------------------------------------------------------ apoyo

    private Usuario buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    private Usuario buscarPropio(UsuarioActual actual) {
        if (actual == null || !actual.identificado()) throw new UnauthorizedException("Debe iniciar sesión.");
        return buscar(actual.id());
    }

    private void aplicarEmail(Usuario u, String emailPedido, Cambios cambios) {
        String email = normalizarEmail(emailPedido);
        if (!email.equals(u.getEmail())) {
            validarEmailDisponible(email);
            cambios.texto("Email", u.getEmail(), email);
            u.setEmail(email);
        }
    }

    private void validarEmailDisponible(String email) {
        if (repo.existsByEmail(email)) throw new ConflictException("El email ya está registrado.");
    }

    private void registrar(Usuario u, UsuarioActual autor, AccionHistorial accion, String detalle) {
        registrar(u, u.getEmail(), autor, accion, detalle);
    }

    private void registrar(Usuario u, String emailUsuario, UsuarioActual autor, AccionHistorial accion, String detalle) {
        UsuarioActual a = autor == null ? UsuarioActual.ANONIMO : autor;
        historialRepo.save(HistorialUsuario.builder()
                .fecha(LocalDateTime.now())
                .idUsuario(u.getId()).emailUsuario(emailUsuario)
                .idAutor(a.id()).emailAutor(a.email()).rolAutor(a.rol())
                .accion(accion)
                .detalle(detalle.length() > 1000 ? detalle.substring(0, 1000) : detalle)
                .build());
    }

    /** Acumula los campos modificados como "Campo: 'antes' → 'después'". */
    private static final class Cambios {
        private final List<String> lista = new ArrayList<>();

        void texto(String campo, String antes, String despues) {
            String a = antes == null ? "" : antes;
            String d = despues == null ? "" : despues;
            if (!Objects.equals(a, d)) lista.add(campo + ": '" + a + "' → '" + d + "'");
        }

        boolean hay() { return !lista.isEmpty(); }

        @Override
        public String toString() { return String.join("; ", lista); }
    }

    private static String estado(Boolean activo) {
        return Boolean.TRUE.equals(activo) ? "activo" : "inactivo";
    }

    private static String nombreCompleto(Usuario u) {
        return (u.getNombre() + " " + textoOVacio(u.getApellido())).trim();
    }

    private Rol parseRol(String rol) {
        try {
            return Rol.valueOf(rol.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Rol inválido: " + rol + ". Valores permitidos: " + Arrays.toString(Rol.values()));
        }
    }

    private static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String textoOVacio(String s) {
        return s != null ? s.trim() : "";
    }

    private UsuarioResponse toResponse(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId()).email(u.getEmail())
                .nombre(u.getNombre()).apellido(u.getApellido())
                .rut(u.getRut()).rol(u.getRol().name())
                .activo(u.getActivo()).build();
    }

    private static HistorialResponse toResponse(HistorialUsuario h) {
        return HistorialResponse.builder()
                .id(h.getId()).fecha(h.getFecha())
                .idUsuario(h.getIdUsuario()).emailUsuario(h.getEmailUsuario())
                .idAutor(h.getIdAutor()).emailAutor(h.getEmailAutor()).rolAutor(h.getRolAutor())
                .accion(h.getAccion().name()).detalle(h.getDetalle())
                .build();
    }

    private UsuarioInternalDTO toInternal(Usuario u) {
        UsuarioInternalDTO dto = new UsuarioInternalDTO();
        dto.setId(u.getId());
        dto.setEmail(u.getEmail());
        dto.setNombre(u.getNombre());
        dto.setApellido(u.getApellido());
        dto.setRut(u.getRut());
        dto.setRol(u.getRol().name());
        dto.setActivo(u.getActivo());
        return dto;
    }
}
