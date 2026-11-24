package cl.pchardware.gateway.security;

/** Identidad extraída de un JWT válido; se guarda como "principal" en el SecurityContext. */
public record UsuarioAutenticado(Long id, String email, String rol, String nombre) {}
