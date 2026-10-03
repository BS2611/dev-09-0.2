package cs151.backend;

import cs151.backend.exception.DuplicateEntityException;
import cs151.backend.exception.ValidationException;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.ProgressEntry;
import cs151.backend.model.Task;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MilestoneTaskProgressTest extends BackendTestBase {

    // ---------------- milestones

    @Test
    void validMilestoneCreated() {
        Goal g = newGoal("G");
        Milestone m = planner.createMilestone(g.getId(), "  M1 ", today.plusDays(5), null);
        assertEquals("M1", planner.getMilestone(m.getId()).getMilestoneName());
        assertEquals("Not Started", m.getStatus());
        assertEquals(1, planner.getMilestonesForGoal(g.getId()).size());
    }

    @Test
    void blankMilestoneNameRejected() {
        Goal g = newGoal("G");
        assertThrows(ValidationException.class, () -> planner.createMilestone(g.getId(), " ", today, null));
    }

    @Test
    void duplicateMilestoneNameInSameGoalRejected() {
        Goal g = newGoal("G");
        newMilestone(g, "M");
        assertThrows(DuplicateEntityException.class, () -> planner.createMilestone(g.getId(), " M", today, null));
    }

    @Test
    void sameMilestoneNameInDifferentGoalsAllowed() {
        newMilestone(newGoal("G1"), "M");
        assertDoesNotThrow(() -> newMilestone(newGoal("G2"), "M"));
    }

    @Test
    void milestoneAfterGoalTargetDateRejected() {
        Goal g = newGoal("G");
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createMilestone(g.getId(), "Late", g.getTargetDate().plusDays(1), null));
        assertEquals("Milestone target date cannot exceed Goal target date.", e.getMessage());
    }

    @Test
    void milestoneForMissingGoalRejected() {
        assertThrows(ValidationException.class, () -> planner.createMilestone(4242, "M", today, null));
    }

    @Test
    void markMilestoneCompleted() {
        Milestone m = newMilestone(newGoal("G"), "M");
        assertEquals("Completed", planner.markMilestoneCompleted(m.getId()).getStatus());
        assertEquals("Completed", planner.getMilestone(m.getId()).getStatus());
    }

    // ---------------- tasks

    @Test
    void validTaskCreatedWithDefaults() {
        Milestone m = newMilestone(newGoal("G"), "M");
        Task t = planner.createTask(m.getId(), "  Write code ", today.plusDays(2), null, null);
        Task loaded = planner.getTask(t.getId());
        assertEquals("Write code", loaded.getDescription());
        assertEquals("Medium", loaded.getPriority());
        assertEquals("To Do", loaded.getStatus());
        assertNull(loaded.getCompletionDate());
    }

    @Test
    void blankTaskDescriptionRejected() {
        Milestone m = newMilestone(newGoal("G"), "M");
        assertThrows(ValidationException.class, () -> planner.createTask(m.getId(), "  ", today, null, null));
    }

    @Test
    void duplicateTaskInSameMilestoneRejected() {
        Milestone m = newMilestone(newGoal("G"), "M");
        newTask(m, "T");
        assertThrows(DuplicateEntityException.class, () -> planner.createTask(m.getId(), "T", today, null, null));
    }

    @Test
    void taskDueAfterMilestoneRejected() {
        Milestone m = newMilestone(newGoal("G"), "M");
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createTask(m.getId(), "T", m.getTargetDate().plusDays(1), null, null));
        assertEquals("Task due date cannot exceed Milestone target date.", e.getMessage());
    }

    @Test
    void markingCompletedSetsCompletionDate() {
        Task t = newTask(newMilestone(newGoal("G"), "M"), "T");
        Task done = planner.markTaskCompleted(t.getId());
        assertEquals(today, done.getCompletionDate());
        assertEquals(today, planner.getTask(t.getId()).getCompletionDate());
    }

    @Test
    void leavingCompletedClearsCompletionDate() {
        Task t = newTask(newMilestone(newGoal("G"), "M"), "T");
        planner.markTaskCompleted(t.getId());
        Task reopened = planner.updateTask(t.getId(), "T", t.getDueDate(), "High", "In Progress");
        assertNull(reopened.getCompletionDate());
        assertNull(planner.getTask(t.getId()).getCompletionDate());
        assertEquals("High", planner.getTask(t.getId()).getPriority());
    }

    @Test
    void createdAsCompletedGetsCompletionDate() {
        Milestone m = newMilestone(newGoal("G"), "M");
        Task t = planner.createTask(m.getId(), "T", today, "Low", "Completed");
        assertEquals(today, t.getCompletionDate());
    }

    @Test
    void invalidPriorityRejected() {
        Milestone m = newMilestone(newGoal("G"), "M");
        assertThrows(ValidationException.class, () -> planner.createTask(m.getId(), "T", today, "Urgent", null));
    }

    // ---------------- progress entries

    @Test
    void validProgressNoteSavedWithAutomaticDate() {
        Task t = newTask(newMilestone(newGoal("G"), "M"), "T");
        ProgressEntry e = planner.addProgressEntry(t.getId(), "  Finished part 1 ");
        assertEquals(today, e.getEntryDate());
        ProgressEntry loaded = planner.getProgressEntriesForTask(t.getId()).get(0);
        assertEquals("Finished part 1", loaded.getNote());
        assertEquals(today, loaded.getEntryDate());
    }

    @Test
    void blankProgressNoteRejected() {
        Task t = newTask(newMilestone(newGoal("G"), "M"), "T");
        ValidationException e = assertThrows(ValidationException.class, () -> planner.addProgressEntry(t.getId(), " "));
        assertEquals("Progress note is required.", e.getMessage());
    }

    @Test
    void updateAndDeleteProgressEntry() {
        Task t = newTask(newMilestone(newGoal("G"), "M"), "T");
        ProgressEntry e = planner.addProgressEntry(t.getId(), "one");
        planner.updateProgressEntry(e.getId(), "two");
        assertEquals("two", planner.getProgressEntriesForTask(t.getId()).get(0).getNote());
        planner.deleteProgressEntry(e.getId());
        assertTrue(planner.getProgressEntriesForTask(t.getId()).isEmpty());
    }
}
