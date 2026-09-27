package cs151.backend.model;

import cs151.backend.exception.ValidationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A personal goal. The id is an internal persistence detail (0 until the goal is saved).
 * The creation date is generated automatically and can never be changed.
 */
public class Goal {
    public static final List<String> CATEGORIES =
            List.of("Academic", "Career", "Health", "Financial", "Personal", "Other");
    public static final List<String> STATUSES =
            List.of("Not Started", "In Progress", "On Hold", "Completed", "Abandoned");
    public static final String STATUS_NOT_STARTED = "Not Started";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_ABANDONED = "Abandoned";

    private long id;
    private String name;
    private String description;
    private String category;
    private LocalDate targetDate;
    private String status;
    private int progress;
    private final LocalDate creationDate;

    /** Creates a new, unsaved goal; the creation date is set to today. */
    public Goal(String name, String description, String category, LocalDate targetDate, String status, int progress) {
        this(0, name, description, category, targetDate, status, progress, LocalDate.now());
    }

    /** Full constructor used when loading a stored goal. */
    public Goal(long id, String name, String description, String category, LocalDate targetDate,
                String status, int progress, LocalDate creationDate) {
        this.id = id;
        setName(name);
        setDescription(description);
        this.category = category;
        this.targetDate = targetDate;
        this.status = status;
        this.progress = progress;
        this.creationDate = creationDate;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    /** Stores the trimmed name (null becomes empty). */
    public void setName(String name) { this.name = name == null ? "" : name.trim(); }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description.trim(); }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getProgress() { return progress; }
    public LocalDate getCreationDate() { return creationDate; }

    /** Sets progress (0-100). A Completed goal must stay at 100. */
    public void updateProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new ValidationException("Progress must be between 0 and 100.");
        }
        if (STATUS_COMPLETED.equals(status) && progress != 100) {
            throw new ValidationException("Completed goals must have 100% progress.");
        }
        this.progress = progress;
    }

    /** Sets status to Completed and progress to 100. */
    public void markCompleted() {
        this.status = STATUS_COMPLETED;
        this.progress = 100;
    }

    /** Sets progress without validation; the service validates before calling this. */
    public void setProgress(int progress) { this.progress = progress; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Goal g)) return false;
        return id != 0 ? id == g.id : g.id == 0 && name.equals(g.name);
    }

    @Override
    public int hashCode() { return id != 0 ? Long.hashCode(id) : Objects.hash(name); }

    @Override
    public String toString() {
        return "Goal{id=" + id + ", name='" + name + "', category=" + category + ", targetDate=" + targetDate
                + ", status=" + status + ", progress=" + progress + ", creationDate=" + creationDate + "}";
    }
}
