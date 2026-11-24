package cl.pchardware.boleta.dto;

/**
 * Identidad del usuario que hace la petición, construida por el API Gateway a partir del JWT
 * (cabeceras X-User-Id, X-User-Email y X-User-Role). Es confiable porque este servicio sólo
 * acepta peticiones con la clave interna.
 */
public record UsuarioActual(Long id, String email, String rol) {

    public boolean esPersonal() {
        return "ADMIN".equals(rol) || "VENDEDOR".equals(rol);
    }

    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }
}
