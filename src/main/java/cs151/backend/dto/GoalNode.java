package cs151.backend.dto;

import cs151.backend.model.Goal;

import java.util.List;

/** A Goal with its milestones; used by both the goal hierarchy report and the selected-goal detail report. */
public record GoalNode(Goal goal, List<MilestoneNode> milestones) {
}
