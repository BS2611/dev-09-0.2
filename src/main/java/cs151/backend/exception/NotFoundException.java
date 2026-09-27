package cs151.backend.exception;

/** Thrown when a Goal, Milestone, Task or ProgressEntry referenced by id does not exist. */
public class NotFoundException extends ValidationException {
    public NotFoundException(String message) {
        super(message);
    }
}
