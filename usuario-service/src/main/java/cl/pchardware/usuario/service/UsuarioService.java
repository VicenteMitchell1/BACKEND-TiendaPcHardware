package cl.pchardware.usuario.service;

import cl.pchardware.usuario.dto.*;
import cl.pchardware.usuario.exception.BadRequestException;
import cl.pchardware.usuario.exception.ConflictException;
import cl.pchardware.usuario.exception.ForbiddenException;
import cl.pchardware.usuario.exception.ResourceNotFoundException;
import cl.pchardware.usuario.exception.UnauthorizedException;
import cl.pchardware.usuario.model.Rol;
import cl.pchardware.usuario.model.Usuario;
import cl.pchardware.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    /** Hash usado cuando el email no existe, para que la respuesta tarde lo mismo (evita enumerar usuarios). */
    private final String hashFicticio;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
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

    @Transactional
    public UsuarioResponse create(UsuarioRequest req) {
        if (!StringUtils.hasText(req.getPassword())) {
            throw new BadRequestException("La contraseña es obligatoria al crear un usuario.");
        }
        String email = normalizarEmail(req.getEmail());
        validarEmailDisponible(email);
        Usuario u = Usuario.builder()
                .email(email)
                .passwordHash(encoder.encode(req.getPassword()))
                .nombre(req.getNombre().trim())
                .apellido(textoOVacio(req.getApellido()))
                .rut(textoOVacio(req.getRut()))
                .rol(StringUtils.hasText(req.getRol()) ? parseRol(req.getRol()) : Rol.CLIENTE)
                .activo(req.getActivo() == null || req.getActivo())
                .build();
        return toResponse(repo.save(u));
    }

    /** Registro público desde la tienda: el rol siempre es CLIENTE, sin importar lo que envíe el cliente. */
    @Transactional
    public UsuarioInternalDTO register(RegisterRequest req) {
        String email = normalizarEmail(req.getEmail());
        validarEmailDisponible(email);
        Usuario u = Usuario.builder()
                .email(email)
                .passwordHash(encoder.encode(req.getPassword()))
                .nombre(req.getNombre().trim())
                .apellido(textoOVacio(req.getApellido()))
                .rut(textoOVacio(req.getRut()))
                .rol(Rol.CLIENTE).activo(true)
                .build();
        return toInternal(repo.save(u));
    }

    @Transactional
    public UsuarioResponse update(Long id, UsuarioRequest req) {
        Usuario u = buscar(id);
        String email = normalizarEmail(req.getEmail());
        if (!email.equals(u.getEmail())) {
            validarEmailDisponible(email);
            u.setEmail(email);
        }
        u.setNombre(req.getNombre().trim());
        if (req.getApellido() != null) u.setApellido(req.getApellido().trim());
        if (req.getRut() != null) u.setRut(req.getRut().trim());
        if (req.getActivo() != null) u.setActivo(req.getActivo());
        if (StringUtils.hasText(req.getRol())) u.setRol(parseRol(req.getRol()));
        if (StringUtils.hasText(req.getPassword())) u.setPasswordHash(encoder.encode(req.getPassword()));
        return toResponse(repo.save(u));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Usuario no encontrado: " + id);
        repo.deleteById(id);
    }

    private Usuario buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    private void validarEmailDisponible(String email) {
        if (repo.existsByEmail(email)) throw new ConflictException("El email ya está registrado.");
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
