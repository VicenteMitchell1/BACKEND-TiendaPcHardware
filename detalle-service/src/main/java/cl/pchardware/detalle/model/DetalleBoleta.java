package cl.pchardware.detalle.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DETALLE_BOLETA")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleBoleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long id;

    @Column(name = "id_boleta", nullable = false)
    private Long idBoleta;

    @Column(name = "id_producto", nullable = false)
    private Long idProducto;

    @Column(name = "nombre_producto", nullable = false, length = 150)
    private String nombreProducto;

    @Column(name = "sku_producto", length = 50)
    private String skuProducto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private Integer precioUnitario;

    @Column(name = "subtotal", nullable = false)
    private Integer subtotal;
}
