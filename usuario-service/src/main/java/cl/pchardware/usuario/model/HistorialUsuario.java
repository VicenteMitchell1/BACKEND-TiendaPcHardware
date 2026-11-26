package cl.pchardware.usuario.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de auditoría: quién cambió qué en una cuenta y cuándo. Nunca guarda contraseñas ni hashes;
 * un cambio de clave queda sólo como acción. No tiene clave foránea al usuario, para conservar el
 * historial aunque la cuenta se elimine (por eso guarda también el email de ese momento).
 */
@Entity
@Table(name = "HISTORIAL_USUARIO", indexes = @Index(name = "idx_historial_usuario", columnList = "id_usuario"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "email_usuario", length = 100)
    private String emailUsuario;

    @Column(name = "id_autor")
    private Long idAutor;

    @Column(name = "email_autor", length = 100)
    private String emailAutor;

    @Column(name = "rol_autor", length = 20)
    private String rolAutor;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 30)
    private AccionHistorial accion;

    @Column(name = "detalle", length = 1000)
    private String detalle;
}
