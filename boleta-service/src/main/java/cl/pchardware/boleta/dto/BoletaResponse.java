package cl.pchardware.boleta.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class BoletaResponse {
    private Long id;
    private String numeroBoleta;
    private LocalDateTime fechaEmision;
    private Integer subtotal;
    private Integer descuento;
    private Integer total;
    private String metodoPago;
    private String estado;
    private String nombreCliente;
    private String emailCliente;
    private String rutCliente;
    private String direccionEnvio;
    private String comuna;
    private String region;
    private Long idUsuario;
    private List<DetalleBoletaDTO> detalles;
}
