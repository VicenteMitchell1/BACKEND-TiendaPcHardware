package cl.pchardware.gateway.dto.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/** Esquemas Swagger de usuarios. */
public final class UsuarioDocs {
    private UsuarioDocs() {}

    @Schema(name = "UsuarioRequest", description = "La contraseña es obligatoria al crear y no se acepta al editar")
    public record Request(
            @Schema(example = "vendedor2@duoc.cl", requiredMode = Schema.RequiredMode.REQUIRED) String email,
            @Schema(example = "clave123", minLength = 4) String password,
            @Schema(example = "Camila", requiredMode = Schema.RequiredMode.REQUIRED) String nombre,
            @Schema(example = "Rojas") String apellido,
            @Schema(example = "19876543-2") String rut,
            @Schema(example = "VENDEDOR", allowableValues = {"ADMIN", "VENDEDOR", "CLIENTE"}) String rol,
            @Schema(example = "true") Boolean activo) {}

    @Schema(name = "Usuario")
    public record Response(
            @Schema(example = "3") Long id,
            @Schema(example = "vendedor@duoc.cl") String email,
            @Schema(example = "Carlos") String nombre,
            @Schema(example = "Vendedor") String apellido,
            @Schema(example = "33333333-3") String rut,
            @Schema(example = "VENDEDOR") String rol,
            @Schema(example = "true") Boolean activo) {}

    @Schema(name = "PerfilRequest", description = "Datos que el usuario puede cambiar de su propia cuenta")
    public record Perfil(
            @Schema(example = "alumno@duoc.cl", requiredMode = Schema.RequiredMode.REQUIRED) String email,
            @Schema(example = "Alumno", requiredMode = Schema.RequiredMode.REQUIRED) String nombre,
            @Schema(example = "Duoc") String apellido) {}

    @Schema(name = "CambioPasswordRequest")
    public record CambioPassword(
            @Schema(example = "1234", requiredMode = Schema.RequiredMode.REQUIRED) String passwordActual,
            @Schema(example = "nueva123", minLength = 4, requiredMode = Schema.RequiredMode.REQUIRED) String passwordNueva) {}

    @Schema(name = "HistorialUsuario", description = "Un cambio en una cuenta. Nunca incluye contraseñas.")
    public record Historial(
            @Schema(example = "12") Long id,
            @Schema(example = "2026-11-26T10:15:30") String fecha,
            @Schema(example = "5") Long idUsuario,
            @Schema(example = "alumno@duoc.cl") String emailUsuario,
            @Schema(example = "1") Long idAutor,
            @Schema(example = "admin@duoc.cl") String emailAutor,
            @Schema(example = "ADMIN") String rolAutor,
            @Schema(example = "EDICION", allowableValues = {"REGISTRO", "CREACION", "EDICION", "CAMBIO_CONTRASENA", "ELIMINACION"}) String accion,
            @Schema(example = "Rol: 'CLIENTE' → 'VENDEDOR'") String detalle) {}
}
