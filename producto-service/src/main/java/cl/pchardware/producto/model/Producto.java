package cl.pchardware.producto.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PRODUCTO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "marca", length = 80)
    private String marca;

    @Column(name = "modelo", length = 80)
    private String modelo;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "precio", nullable = false)
    private Integer precio;

    @Column(name = "precio_anterior")
    private Integer precioAnterior;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "imagen", length = 255)
    private String imagen;

    @Column(name = "es_oferta")
    @Builder.Default
    private Boolean esOferta = false;

    @Column(name = "porcentaje_descuento")
    private Integer porcentajeDescuento;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "id_categoria", nullable = false)
    private Long idCategoria;

    @Column(name = "nombre_categoria", length = 100)
    private String nombreCategoria;
}
