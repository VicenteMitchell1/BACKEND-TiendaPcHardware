package cl.pchardware.usuario.dto;

/**
 * Identidad de quien hace la petición, construida por el API Gateway a partir del JWT
 * (cabeceras X-User-Id, X-User-Email y X-User-Role). Es confiable porque este servicio sólo
 * acepta peticiones con la clave interna. Los campos son null si la petición no trae sesión.
 */
public record UsuarioActual(Long id, String email, String rol) {

    public static final UsuarioActual ANONIMO = new UsuarioActual(null, null, null);

    public boolean identificado() {
        return id != null;
    }
}
