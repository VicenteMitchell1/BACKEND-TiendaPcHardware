package cl.pchardware.boleta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BoletaRequest {
    private Integer subtotal;
    private Integer descuento;

    @NotNull(message = "El total de la boleta es obligatorio")
    private Integer total;

    private String metodoPago;
    private String nombreCliente;
    private String emailCliente;
    private String rutCliente;
    private String direccionEnvio;
    private String comuna;
    private String region;
    private Long idUsuario;

    @NotEmpty(message = "La boleta debe incluir al menos un producto")
    @Valid
    private List<DetalleBoletaDTO> items;
}
