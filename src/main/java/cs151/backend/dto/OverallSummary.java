package cs151.backend.dto;

/**
 * Overall totals. Active goals are those neither Completed nor Abandoned. Completion rate is a percentage
 * (0-100) of goals that are Completed, or 0 when there are no goals.
 */
public record OverallSummary(int totalGoals, int completedGoals, int activeGoals, double completionRate,
                             int totalTasks) {
}
