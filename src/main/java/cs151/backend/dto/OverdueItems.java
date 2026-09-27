package cs151.backend.dto;

import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.Task;

import java.util.List;

/** Incomplete goals, milestones and tasks whose date has already passed. */
public record OverdueItems(List<Goal> goals, List<Milestone> milestones, List<Task> tasks) {
}
