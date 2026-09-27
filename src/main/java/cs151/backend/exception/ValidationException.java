package cs151.backend.exception;

/**
 * Thrown when user-supplied data breaks a business rule. The message is written to be shown to the user as-is.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
