package cl.pchardware.boleta.exception;

/** Se traduce a HTTP 403 en {@link GlobalExceptionHandler}. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}
