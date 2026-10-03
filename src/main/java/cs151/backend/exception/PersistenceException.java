package cs151.backend.exception;

/** Thrown when the database cannot be opened, read or written (an unexpected failure, not a user error). */
public class PersistenceException extends RuntimeException {
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
