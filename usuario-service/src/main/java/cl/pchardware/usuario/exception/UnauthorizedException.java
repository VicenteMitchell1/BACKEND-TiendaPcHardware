package cl.pchardware.usuario.exception;

/** Se traduce a HTTP 401 en {@link GlobalExceptionHandler}. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}
