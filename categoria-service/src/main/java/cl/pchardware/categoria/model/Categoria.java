package cl.pchardware.categoria.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CATEGORIA")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "icono", length = 50)
    private String icono;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
