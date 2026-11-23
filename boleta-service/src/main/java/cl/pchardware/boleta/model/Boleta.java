package cl.pchardware.boleta.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "BOLETA")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_boleta")
    private Long id;

    @Column(name = "numero_boleta", nullable = false, unique = true, length = 30)
    private String numeroBoleta;

    @Column(name = "fecha_emision", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEmision = LocalDateTime.now();

    @Column(name = "subtotal", nullable = false)
    private Integer subtotal;

    @Column(name = "descuento")
    @Builder.Default
    private Integer descuento = 0;

    @Column(name = "total", nullable = false)
    private Integer total;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "PAGADA";

    @Column(name = "nombre_cliente", length = 100)
    private String nombreCliente;

    @Column(name = "email_cliente", length = 100)
    private String emailCliente;

    @Column(name = "rut_cliente", length = 20)
    private String rutCliente;

    @Column(name = "direccion_envio", length = 200)
    private String direccionEnvio;

    @Column(name = "comuna", length = 80)
    private String comuna;

    @Column(name = "region", length = 80)
    private String region;

    @Column(name = "id_usuario")
    private Long idUsuario;
}
