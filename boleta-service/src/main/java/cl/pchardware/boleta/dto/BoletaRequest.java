package cl.pchardware.boleta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Solicitud de compra. Los montos (subtotal, descuento, total) y el usuario NO se toman de aquí:
 * el servidor los calcula con los precios del catálogo y la identidad del token JWT.
 * Esos campos se aceptan sólo por compatibilidad con clientes antiguos y se ignoran.
 */
@Data
public class BoletaRequest {
    @Schema(description = "Ignorado: lo calcula el servidor", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer subtotal;
    @Schema(description = "Ignorado: lo calcula el servidor", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer descuento;
    @Schema(description = "Ignorado: lo calcula el servidor", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer total;

    @Schema(description = "Código de cupón opcional; el servidor lo valida y calcula el descuento", example = "HARDWARE2026")
    @Size(max = 30, message = "El código de cupón admite hasta 30 caracteres")
    private String codigoCupon;

    private String metodoPago;
    private String nombreCliente;
    @Schema(description = "Ignorado: se usa el email del token", accessMode = Schema.AccessMode.READ_ONLY)
    private String emailCliente;
    private String rutCliente;
    private String direccionEnvio;
    private String comuna;
    private String region;
    @Schema(description = "Ignorado: se usa el usuario del token", accessMode = Schema.AccessMode.READ_ONLY)
    private Long idUsuario;

    @NotEmpty(message = "La boleta debe incluir al menos un producto")
    @Valid
    private List<DetalleBoletaDTO> items;
}
