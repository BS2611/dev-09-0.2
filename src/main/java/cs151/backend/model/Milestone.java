package cs151.backend.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** A milestone that belongs to exactly one Goal. */
public class Milestone {
    public static final List<String> STATUSES = List.of("Not Started", "In Progress", "Completed", "Skipped");
    public static final String STATUS_NOT_STARTED = "Not Started";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_SKIPPED = "Skipped";

    private long id;
    private final long goalId;
    private String milestoneName;
    private LocalDate targetDate;
    private String status;

    public Milestone(long goalId, String milestoneName, LocalDate targetDate, String status) {
        this(0, goalId, milestoneName, targetDate, status);
    }

    /** Full constructor used when loading a stored milestone. */
    public Milestone(long id, long goalId, String milestoneName, LocalDate targetDate, String status) {
        this.id = id;
        this.goalId = goalId;
        setMilestoneName(milestoneName);
        this.targetDate = targetDate;
        this.status = status;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getGoalId() { return goalId; }

    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) {
        this.milestoneName = milestoneName == null ? "" : milestoneName.trim();
    }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public void markCompleted() { this.status = STATUS_COMPLETED; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Milestone m)) return false;
        return id != 0 ? id == m.id : m.id == 0 && goalId == m.goalId && milestoneName.equals(m.milestoneName);
    }

    @Override
    public int hashCode() { return id != 0 ? Long.hashCode(id) : Objects.hash(goalId, milestoneName); }

    @Override
    public String toString() {
        return "Milestone{id=" + id + ", goalId=" + goalId + ", name='" + milestoneName + "', targetDate="
                + targetDate + ", status=" + status + "}";
    }
}
