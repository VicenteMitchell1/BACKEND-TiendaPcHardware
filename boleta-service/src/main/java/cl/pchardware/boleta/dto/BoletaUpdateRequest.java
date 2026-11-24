package cl.pchardware.boleta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Edición administrativa de una boleta. Los montos y los items no se editan: para eso se anula la compra. */
@Data
public class BoletaUpdateRequest {
    @Pattern(regexp = "PAGADA|ENVIADA|ENTREGADA|ANULADA", message = "Estado inválido: use PAGADA, ENVIADA, ENTREGADA o ANULADA")
    @Schema(description = "Al pasar a ANULADA se repone el stock de los productos", example = "ENVIADA")
    private String estado;
    private String metodoPago;
    private String nombreCliente;
    private String rutCliente;
    private String direccionEnvio;
    private String comuna;
    private String region;
}
