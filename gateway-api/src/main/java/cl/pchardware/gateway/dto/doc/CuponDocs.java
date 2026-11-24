package cl.pchardware.gateway.dto.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/** Esquemas Swagger de cupones de descuento. */
public final class CuponDocs {
    private CuponDocs() {}

    @Schema(name = "CuponRequest")
    public record Request(
            @Schema(example = "VERANO20", description = "3-30 caracteres: letras, números, - y _. Se guarda en mayúsculas",
                    requiredMode = Schema.RequiredMode.REQUIRED) String codigo,
            @Schema(example = "20", minimum = "1", maximum = "90", requiredMode = Schema.RequiredMode.REQUIRED) Integer porcentaje,
            @Schema(example = "20% de descuento de verano") String descripcion,
            @Schema(example = "true") Boolean activo) {}

    @Schema(name = "Cupon")
    public record Response(
            @Schema(example = "1") Long id,
            @Schema(example = "HARDWARE2026") String codigo,
            @Schema(example = "15") Integer porcentaje,
            @Schema(example = "15% de descuento especial hardware 2026") String descripcion,
            @Schema(example = "true") Boolean activo) {}
}
