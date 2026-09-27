package cs151.backend.exception;

/** Thrown when a name/description that must be unique already exists. */
public class DuplicateEntityException extends ValidationException {
    public DuplicateEntityException(String message) {
        super(message);
    }
}
