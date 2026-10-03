package cs151.backend.dto;

import cs151.backend.model.ProgressEntry;
import cs151.backend.model.Task;

import java.util.List;

/** A Task with its progress entries (leaf of the goal hierarchy report). */
public record TaskNode(Task task, List<ProgressEntry> progressEntries) {
}
