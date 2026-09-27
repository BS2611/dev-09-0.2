package cs151.backend.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** A task that belongs to exactly one Milestone. */
public class Task {
    public static final List<String> PRIORITIES = List.of("Low", "Medium", "High");
    public static final List<String> STATUSES = List.of("To Do", "In Progress", "Completed", "Cancelled");
    public static final String DEFAULT_PRIORITY = "Medium";
    public static final String STATUS_TO_DO = "To Do";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CANCELLED = "Cancelled";

    private long id;
    private final long milestoneId;
    private String description;
    private LocalDate dueDate;
    private String priority;
    private String status;
    private LocalDate completionDate;

    /** Creates a new task. If the status is Completed the completion date is set to today. */
    public Task(long milestoneId, String description, LocalDate dueDate, String priority, String status) {
        this(0, milestoneId, description, dueDate, priority, status,
                STATUS_COMPLETED.equals(status) ? LocalDate.now() : null);
    }

    /** Full constructor used when loading a stored task. */
    public Task(long id, long milestoneId, String description, LocalDate dueDate, String priority,
                String status, LocalDate completionDate) {
        this.id = id;
        this.milestoneId = milestoneId;
        setDescription(description);
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
        this.completionDate = completionDate;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMilestoneId() { return milestoneId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description.trim(); }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }

    /**
     * Changes the status and keeps the completion date consistent: moving to Completed sets it to today
     * (unless already Completed), moving away from Completed clears it.
     */
    public void setStatus(String newStatus) {
        boolean wasCompleted = STATUS_COMPLETED.equals(status);
        boolean nowCompleted = STATUS_COMPLETED.equals(newStatus);
        if (nowCompleted && !wasCompleted) {
            completionDate = LocalDate.now();
        } else if (!nowCompleted) {
            completionDate = null;
        }
        this.status = newStatus;
    }

    public LocalDate getCompletionDate() { return completionDate; }

    public void markCompleted() { setStatus(STATUS_COMPLETED); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task t)) return false;
        return id != 0 ? id == t.id : t.id == 0 && milestoneId == t.milestoneId && description.equals(t.description);
    }

    @Override
    public int hashCode() { return id != 0 ? Long.hashCode(id) : Objects.hash(milestoneId, description); }

    @Override
    public String toString() {
        return "Task{id=" + id + ", milestoneId=" + milestoneId + ", description='" + description + "', dueDate="
                + dueDate + ", priority=" + priority + ", status=" + status + ", completionDate=" + completionDate + "}";
    }
}
