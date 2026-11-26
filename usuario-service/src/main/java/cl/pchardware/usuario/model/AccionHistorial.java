package cl.pchardware.usuario.model;

public enum AccionHistorial {
    /** Alta desde el registro público de la tienda. */
    REGISTRO,
    /** Alta por un administrador. */
    CREACION,
    /** Cambio de datos de la cuenta (el detalle lista cada campo con su valor anterior y nuevo). */
    EDICION,
    /** Cambio de contraseña hecho por el propio usuario (sin guardar la clave). */
    CAMBIO_CONTRASENA,
    ELIMINACION
}
