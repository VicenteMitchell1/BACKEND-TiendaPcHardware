package cl.pchardware.boleta.model;

import jakarta.persistence.*;
import lombok.*;

/** Cupón de descuento porcentual que se aplica al subtotal de una boleta. El código se guarda en mayúsculas. */
@Entity
@Table(name = "CUPON")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cupon")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "porcentaje", nullable = false)
    private Integer porcentaje;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
