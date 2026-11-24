package cl.pchardware.usuario.exception;

/** Se traduce a HTTP 400 en {@link GlobalExceptionHandler}. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
