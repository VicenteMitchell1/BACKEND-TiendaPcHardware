package cl.pchardware.categoria.exception;

/** Se traduce a HTTP 409 en {@link GlobalExceptionHandler}. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
