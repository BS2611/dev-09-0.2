package cs151.backend.dto;

import cs151.backend.model.Milestone;

import java.util.List;

/** A Milestone with its tasks. */
public record MilestoneNode(Milestone milestone, List<TaskNode> tasks) {
}
