package cs151.backend.service;

import cs151.backend.dto.SearchResults;
import cs151.backend.exception.DuplicateEntityException;
import cs151.backend.exception.NotFoundException;
import cs151.backend.exception.ValidationException;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.ProgressEntry;
import cs151.backend.model.Task;
import cs151.backend.persistence.DataManager;

import java.time.LocalDate;
import java.util.List;

/**
 * Main backend API for the frontend. Validates input, enforces the business rules and delegates storage to
 * {@link DataManager}. Every method either returns the saved/loaded object or throws a
 * {@link ValidationException} (or subclass) whose message can be shown to the user directly.
 */
public class GoalPlanner {
    private final DataManager dataManager;

    /** Uses the default database file. */
    public GoalPlanner() {
        this(new DataManager());
    }

    public GoalPlanner(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    // ------------------------------------------------------------------ goals

    /** Creates and saves a goal. A null/blank status defaults to "Not Started". Creation date is automatic. */
    public Goal createGoal(String name, String description, String category, LocalDate targetDate,
                           String status, int progress) {
        Goal goal = new Goal(name, description, category, targetDate, defaultIfBlank(status, Goal.STATUS_NOT_STARTED),
                progress);
        validateGoal(goal);
        requireTargetDateNotPast(goal.getTargetDate());
        dataManager.insertGoal(goal);
        return goal;
    }

    /** Creates a goal with status "Not Started" and progress 0. */
    public Goal createGoal(String name, String description, String category, LocalDate targetDate) {
        return createGoal(name, description, category, targetDate, Goal.STATUS_NOT_STARTED, 0);
    }

    /**
     * Updates a goal. The "today or later" rule only applies if the target date is being changed, so an
     * existing goal whose date has passed can still be edited. The target date may not move before the
     * latest milestone target date.
     */
    public Goal updateGoal(long goalId, String name, String description, String category, LocalDate targetDate,
                           String status, int progress) {
        Goal goal = getGoal(goalId);
        LocalDate oldTargetDate = goal.getTargetDate();
        goal.setName(name);
        goal.setDescription(description);
        goal.setCategory(category);
        goal.setTargetDate(targetDate);
        goal.setStatus(defaultIfBlank(status, Goal.STATUS_NOT_STARTED));
        goal.setProgress(progress);
        validateGoal(goal);
        if (!goal.getTargetDate().equals(oldTargetDate)) {
            requireTargetDateNotPast(goal.getTargetDate());
        }
        for (Milestone m : dataManager.getMilestonesForGoal(goalId)) {
            if (m.getTargetDate().isAfter(goal.getTargetDate())) {
                throw new ValidationException("Goal target date cannot be earlier than a milestone target date.");
            }
        }
        dataManager.updateGoal(goal);
        return goal;
    }

    /** Deletes the goal and, by cascade, its milestones, tasks and progress entries. */
    public void deleteGoal(long goalId) {
        if (!dataManager.deleteGoal(goalId)) throw new NotFoundException("Goal not found.");
    }

    public Goal getGoal(long goalId) {
        return dataManager.findGoalById(goalId).orElseThrow(() -> new NotFoundException("Goal not found."));
    }

    public List<Goal> getAllGoals() {
        return dataManager.getAllGoals();
    }

    /** Sets a goal's progress (0-100); a Completed goal must remain at 100. */
    public Goal updateGoalProgress(long goalId, int progress) {
        Goal goal = getGoal(goalId);
        goal.updateProgress(progress);
        dataManager.updateGoal(goal);
        return goal;
    }

    /** Sets status to Completed and progress to 100. */
    public Goal markGoalCompleted(long goalId) {
        Goal goal = getGoal(goalId);
        goal.markCompleted();
        dataManager.updateGoal(goal);
        return goal;
    }

    private void validateGoal(Goal goal) {
        if (goal.getName().isEmpty()) throw new ValidationException("Goal name is required.");
        dataManager.findGoalByName(goal.getName()).ifPresent(existing -> {
            if (existing.getId() != goal.getId()) throw new DuplicateEntityException("Goal name already exists.");
        });
        if (goal.getCategory() == null || !Goal.CATEGORIES.contains(goal.getCategory())) {
            throw new ValidationException("Category must be one of: " + String.join(", ", Goal.CATEGORIES) + ".");
        }
        if (goal.getTargetDate() == null) throw new ValidationException("Target date is required.");
        if (!Goal.STATUSES.contains(goal.getStatus())) {
            throw new ValidationException("Status must be one of: " + String.join(", ", Goal.STATUSES) + ".");
        }
        if (goal.getProgress() < 0 || goal.getProgress() > 100) {
            throw new ValidationException("Progress must be between 0 and 100.");
        }
        if (Goal.STATUS_COMPLETED.equals(goal.getStatus()) && goal.getProgress() != 100) {
            throw new ValidationException("Completed goals must have 100% progress.");
        }
    }

    private void requireTargetDateNotPast(LocalDate date) {
        if (date.isBefore(LocalDate.now())) throw new ValidationException("Target date must be today or later.");
    }

    // ------------------------------------------------------------------ milestones

    /** Creates a milestone under a goal. A null/blank status defaults to "Not Started". */
    public Milestone createMilestone(long goalId, String name, LocalDate targetDate, String status) {
        Goal goal = getGoal(goalId);
        Milestone m = new Milestone(goalId, name, targetDate, defaultIfBlank(status, Milestone.STATUS_NOT_STARTED));
        validateMilestone(m, goal);
        dataManager.insertMilestone(m);
        return m;
    }

    /** Updates a milestone's name, target date and status; its goal cannot change. */
    public Milestone updateMilestone(long milestoneId, String name, LocalDate targetDate, String status) {
        Milestone m = getMilestone(milestoneId);
        m.setMilestoneName(name);
        m.setTargetDate(targetDate);
        m.setStatus(defaultIfBlank(status, Milestone.STATUS_NOT_STARTED));
        validateMilestone(m, getGoal(m.getGoalId()));
        for (Task t : dataManager.getTasksForMilestone(milestoneId)) {
            if (t.getDueDate().isAfter(m.getTargetDate())) {
                throw new ValidationException("Milestone target date cannot be earlier than a task due date.");
            }
        }
        dataManager.updateMilestone(m);
        return m;
    }

    /** Deletes the milestone and, by cascade, its tasks and their progress entries. */
    public void deleteMilestone(long milestoneId) {
        if (!dataManager.deleteMilestone(milestoneId)) throw new NotFoundException("Milestone not found.");
    }

    public Milestone getMilestone(long milestoneId) {
        return dataManager.findMilestoneById(milestoneId)
                .orElseThrow(() -> new NotFoundException("Milestone not found."));
    }

    public List<Milestone> getMilestonesForGoal(long goalId) {
        return dataManager.getMilestonesForGoal(goalId);
    }

    public Milestone markMilestoneCompleted(long milestoneId) {
        Milestone m = getMilestone(milestoneId);
        m.markCompleted();
        dataManager.updateMilestone(m);
        return m;
    }

    private void validateMilestone(Milestone m, Goal goal) {
        if (m.getMilestoneName().isEmpty()) throw new ValidationException("Milestone name is required.");
        for (Milestone other : dataManager.getMilestonesForGoal(goal.getId())) {
            if (other.getId() != m.getId() && other.getMilestoneName().equals(m.getMilestoneName())) {
                throw new DuplicateEntityException("Milestone name already exists for this goal.");
            }
        }
        if (m.getTargetDate() == null) throw new ValidationException("Milestone target date is required.");
        if (m.getTargetDate().isAfter(goal.getTargetDate())) {
            throw new ValidationException("Milestone target date cannot exceed Goal target date.");
        }
        if (!Milestone.STATUSES.contains(m.getStatus())) {
            throw new ValidationException("Milestone status must be one of: "
                    + String.join(", ", Milestone.STATUSES) + ".");
        }
    }

    // ------------------------------------------------------------------ tasks

    /** Creates a task under a milestone. Null/blank priority defaults to Medium, status to "To Do". */
    public Task createTask(long milestoneId, String description, LocalDate dueDate, String priority, String status) {
        Milestone milestone = getMilestone(milestoneId);
        Task t = new Task(milestoneId, description, dueDate, defaultIfBlank(priority, Task.DEFAULT_PRIORITY),
                defaultIfBlank(status, Task.STATUS_TO_DO));
        validateTask(t, milestone);
        dataManager.insertTask(t);
        return t;
    }

    /**
     * Updates a task. Moving status to Completed sets the completion date to today; moving away from
     * Completed clears it.
     */
    public Task updateTask(long taskId, String description, LocalDate dueDate, String priority, String status) {
        Task t = getTask(taskId);
        t.setDescription(description);
        t.setDueDate(dueDate);
        t.setPriority(defaultIfBlank(priority, Task.DEFAULT_PRIORITY));
        t.setStatus(defaultIfBlank(status, Task.STATUS_TO_DO));
        validateTask(t, getMilestone(t.getMilestoneId()));
        dataManager.updateTask(t);
        return t;
    }

    /** Deletes the task and, by cascade, its progress entries. */
    public void deleteTask(long taskId) {
        if (!dataManager.deleteTask(taskId)) throw new NotFoundException("Task not found.");
    }

    public Task getTask(long taskId) {
        return dataManager.findTaskById(taskId).orElseThrow(() -> new NotFoundException("Task not found."));
    }

    public List<Task> getTasksForMilestone(long milestoneId) {
        return dataManager.getTasksForMilestone(milestoneId);
    }

    public Task markTaskCompleted(long taskId) {
        Task t = getTask(taskId);
        t.markCompleted();
        dataManager.updateTask(t);
        return t;
    }

    private void validateTask(Task t, Milestone milestone) {
        if (t.getDescription().isEmpty()) throw new ValidationException("Task description is required.");
        for (Task other : dataManager.getTasksForMilestone(milestone.getId())) {
            if (other.getId() != t.getId() && other.getDescription().equals(t.getDescription())) {
                throw new DuplicateEntityException("Task description already exists for this milestone.");
            }
        }
        if (t.getDueDate() == null) throw new ValidationException("Task due date is required.");
        if (t.getDueDate().isAfter(milestone.getTargetDate())) {
            throw new ValidationException("Task due date cannot exceed Milestone target date.");
        }
        if (!Task.PRIORITIES.contains(t.getPriority())) {
            throw new ValidationException("Priority must be one of: " + String.join(", ", Task.PRIORITIES) + ".");
        }
        if (!Task.STATUSES.contains(t.getStatus())) {
            throw new ValidationException("Task status must be one of: " + String.join(", ", Task.STATUSES) + ".");
        }
    }

    // ------------------------------------------------------------------ progress entries

    /** Adds a note to a task. The entry date is always today and cannot be chosen by the caller. */
    public ProgressEntry addProgressEntry(long taskId, String note) {
        getTask(taskId);
        ProgressEntry entry = new ProgressEntry(taskId, note);
        requireNote(entry);
        dataManager.insertProgressEntry(entry);
        return entry;
    }

    /** Changes the note of an entry; its date stays as it was. */
    public ProgressEntry updateProgressEntry(long entryId, String note) {
        ProgressEntry entry = dataManager.findProgressEntryById(entryId)
                .orElseThrow(() -> new NotFoundException("Progress entry not found."));
        entry.setNote(note);
        requireNote(entry);
        dataManager.updateProgressEntry(entry);
        return entry;
    }

    public void deleteProgressEntry(long entryId) {
        if (!dataManager.deleteProgressEntry(entryId)) throw new NotFoundException("Progress entry not found.");
    }

    public List<ProgressEntry> getProgressEntriesForTask(long taskId) {
        return dataManager.getProgressEntriesForTask(taskId);
    }

    private void requireNote(ProgressEntry entry) {
        if (entry.getNote().isEmpty()) throw new ValidationException("Progress note is required.");
    }

    // ------------------------------------------------------------------ search

    /**
     * Global, case-insensitive search. Goals match on name, description, category or status; milestones on
     * name or status; tasks on description, priority or status. A blank query returns no results.
     */
    public SearchResults search(String query) {
        if (query == null || query.isBlank()) return new SearchResults(List.of(), List.of(), List.of());
        return new SearchResults(dataManager.searchGoals(query), dataManager.searchMilestones(query),
                dataManager.searchTasks(query));
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
