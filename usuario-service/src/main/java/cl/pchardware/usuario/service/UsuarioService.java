package cl.pchardware.usuario.service;

import cl.pchardware.usuario.dto.*;
import cl.pchardware.usuario.exception.BadRequestException;
import cl.pchardware.usuario.exception.ResourceNotFoundException;
import cl.pchardware.usuario.model.Rol;
import cl.pchardware.usuario.model.Usuario;
import cl.pchardware.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional(readOnly = true)
    public List<UsuarioResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse findById(Long id) {
        return toResponse(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id)));
    }

    @Transactional(readOnly = true)
    public UsuarioInternalDTO findByEmailInternal(String email) {
        Usuario u = repo.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
        UsuarioInternalDTO dto = new UsuarioInternalDTO();
        dto.setId(u.getId());
        dto.setEmail(u.getEmail());
        dto.setPasswordHash(u.getPasswordHash());
        dto.setNombre(u.getNombre());
        dto.setApellido(u.getApellido());
        dto.setRut(u.getRut());
        dto.setRol(u.getRol().name());
        dto.setActivo(u.getActivo());
        return dto;
    }

    @Transactional
    public UsuarioResponse create(UsuarioRequest req) {
        if (repo.existsByEmail(req.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("El email ya está registrado.");
        }
        Rol rol = Rol.CLIENTE;
        if (StringUtils.hasText(req.getRol())) {
            try { rol = Rol.valueOf(req.getRol().toUpperCase()); } catch (Exception ignored) {}
        }
        Usuario u = Usuario.builder()
                .email(req.getEmail().trim().toLowerCase())
                .passwordHash(encoder.encode(req.getPassword()))
                .nombre(req.getNombre().trim())
                .apellido(req.getApellido() != null ? req.getApellido().trim() : "")
                .rut(req.getRut() != null ? req.getRut().trim() : "")
                .rol(rol).activo(true)
                .build();
        return toResponse(repo.save(u));
    }

    @Transactional
    public UsuarioResponse register(RegisterRequest req) {
        if (repo.existsByEmail(req.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("El email ya está registrado.");
        }
        Usuario u = Usuario.builder()
                .email(req.getEmail().trim().toLowerCase())
                .passwordHash(encoder.encode(req.getPassword()))
                .nombre(req.getNombre().trim())
                .apellido(req.getApellido() != null ? req.getApellido().trim() : "")
                .rut(req.getRut() != null ? req.getRut().trim() : "")
                .rol(Rol.CLIENTE).activo(true)
                .build();
        return toResponse(repo.save(u));
    }

    @Transactional
    public UsuarioResponse update(Long id, UsuarioRequest req) {
        Usuario u = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        u.setNombre(req.getNombre().trim());
        if (req.getApellido() != null) u.setApellido(req.getApellido().trim());
        if (req.getRut() != null) u.setRut(req.getRut().trim());
        if (req.getActivo() != null) u.setActivo(req.getActivo());
        if (StringUtils.hasText(req.getRol())) {
            try { u.setRol(Rol.valueOf(req.getRol().toUpperCase())); } catch (Exception ignored) {}
        }
        if (StringUtils.hasText(req.getPassword())) {
            u.setPasswordHash(encoder.encode(req.getPassword()));
        }
        return toResponse(repo.save(u));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Usuario no encontrado: " + id);
        repo.deleteById(id);
    }

    private UsuarioResponse toResponse(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId()).email(u.getEmail())
                .nombre(u.getNombre()).apellido(u.getApellido())
                .rut(u.getRut()).rol(u.getRol().name())
                .activo(u.getActivo()).build();
    }
}
