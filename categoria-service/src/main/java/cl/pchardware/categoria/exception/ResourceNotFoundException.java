package cl.pchardware.categoria.exception;

/** Se traduce a HTTP 404 en {@link GlobalExceptionHandler}. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
