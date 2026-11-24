package cl.pchardware.gateway.dto.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/** Formato común de error de toda la API (sólo documentación Swagger). */
@Schema(name = "Error", description = "Respuesta de error estándar")
public record ErrorDoc(
        @Schema(example = "2026-11-24T10:15:30") String timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Datos de entrada inválidos.") String error,
        @Schema(description = "Errores por campo (sólo en validaciones)", example = "{\"precio\": \"El precio no puede ser negativo\"}") Object detalles) {}
