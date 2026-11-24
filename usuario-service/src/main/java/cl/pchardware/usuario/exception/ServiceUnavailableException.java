package cl.pchardware.usuario.exception;

/** Se traduce a HTTP 503 en {@link GlobalExceptionHandler}. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) { super(message); }
}
