package cs151.backend.dto;

import java.time.LocalDate;

/** One row of the goals overview report. */
public record GoalOverviewRow(long goalId, String name, String category, String status, int progress,
                              LocalDate targetDate, int milestoneCount, int taskCount) {
}
