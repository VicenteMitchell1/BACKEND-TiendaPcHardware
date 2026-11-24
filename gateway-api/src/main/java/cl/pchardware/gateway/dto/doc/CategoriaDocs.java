package cl.pchardware.gateway.dto.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/** Esquemas Swagger de categorías. */
public final class CategoriaDocs {
    private CategoriaDocs() {}

    @Schema(name = "CategoriaRequest")
    public record Request(
            @Schema(example = "Fuentes de Poder", requiredMode = Schema.RequiredMode.REQUIRED) String nombre,
            @Schema(example = "Fuentes certificadas 80 Plus") String descripcion,
            @Schema(example = "bi-lightning-charge") String icono,
            @Schema(example = "true") Boolean activo) {}

    @Schema(name = "Categoria")
    public record Response(
            @Schema(example = "1") Long id,
            @Schema(example = "Tarjetas Gráficas") String nombre,
            @Schema(example = "Potencia gráfica para gaming extremo y renderizado 3D") String descripcion,
            @Schema(example = "bi-gpu-card") String icono,
            @Schema(example = "true") Boolean activo) {}
}
