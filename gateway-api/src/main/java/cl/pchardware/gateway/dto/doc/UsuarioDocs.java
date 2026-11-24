package cl.pchardware.gateway.dto.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/** Esquemas Swagger de usuarios. */
public final class UsuarioDocs {
    private UsuarioDocs() {}

    @Schema(name = "UsuarioRequest", description = "La contraseña es obligatoria al crear y opcional al editar")
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
}
