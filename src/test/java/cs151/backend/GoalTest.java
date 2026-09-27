package cs151.backend;

import cs151.backend.exception.DuplicateEntityException;
import cs151.backend.exception.ValidationException;
import cs151.backend.model.Goal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoalTest extends BackendTestBase {

    @Test
    void validGoalIsCreatedAndSaved() {
        Goal g = planner.createGoal("  Learn SQL  ", "desc", "Career", today, "In Progress", 40);
        assertTrue(g.getId() > 0);
        Goal loaded = planner.getGoal(g.getId());
        assertEquals("Learn SQL", loaded.getName());
        assertEquals("Career", loaded.getCategory());
        assertEquals("In Progress", loaded.getStatus());
        assertEquals(40, loaded.getProgress());
        assertEquals(today, loaded.getTargetDate());
    }

    @Test
    void defaultsAreNotStartedAndZero() {
        Goal g = planner.createGoal("Defaults", null, "Other", today.plusDays(1));
        assertEquals("Not Started", g.getStatus());
        assertEquals(0, g.getProgress());
    }

    @Test
    void blankNameRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createGoal("   ", "", "Other", today, "Not Started", 0));
        assertEquals("Goal name is required.", e.getMessage());
    }

    @Test
    void duplicateTrimmedNameRejected() {
        newGoal("Run a marathon");
        DuplicateEntityException e = assertThrows(DuplicateEntityException.class,
                () -> planner.createGoal("  Run a marathon ", "", "Health", today, "Not Started", 0));
        assertEquals("Goal name already exists.", e.getMessage());
    }

    @Test
    void pastTargetDateRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createGoal("Old", "", "Other", today.minusDays(1), "Not Started", 0));
        assertEquals("Target date must be today or later.", e.getMessage());
    }

    @Test
    void invalidCategoryAndStatusRejected() {
        assertThrows(ValidationException.class,
                () -> planner.createGoal("A", "", "Hobby", today, "Not Started", 0));
        assertThrows(ValidationException.class,
                () -> planner.createGoal("B", "", "Other", today, "Done", 0));
    }

    @Test
    void progressBelowZeroRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createGoal("Neg", "", "Other", today, "Not Started", -1));
        assertEquals("Progress must be between 0 and 100.", e.getMessage());
    }

    @Test
    void progressAboveHundredRejected() {
        assertThrows(ValidationException.class,
                () -> planner.createGoal("Big", "", "Other", today, "In Progress", 101));
    }

    @Test
    void completedWithoutFullProgressRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createGoal("Almost", "", "Other", today, "Completed", 99));
        assertEquals("Completed goals must have 100% progress.", e.getMessage());
    }

    @Test
    void validCompletedGoalSaved() {
        Goal g = planner.createGoal("Done", "", "Other", today, "Completed", 100);
        assertEquals("Completed", planner.getGoal(g.getId()).getStatus());
        assertEquals(100, planner.getGoal(g.getId()).getProgress());
    }

    @Test
    void creationDateGeneratedAutomatically() {
        Goal g = newGoal("Dated");
        assertEquals(today, g.getCreationDate());
        assertEquals(today, planner.getGoal(g.getId()).getCreationDate());
    }

    @Test
    void progressUpdateAndCompletion() {
        Goal g = newGoal("Progressing");
        assertEquals(60, planner.updateGoalProgress(g.getId(), 60).getProgress());
        assertThrows(ValidationException.class, () -> planner.updateGoalProgress(g.getId(), 101));

        Goal done = planner.markGoalCompleted(g.getId());
        assertEquals("Completed", done.getStatus());
        assertEquals(100, planner.getGoal(g.getId()).getProgress());
        assertThrows(ValidationException.class, () -> planner.updateGoalProgress(g.getId(), 50));
    }

    @Test
    void updateGoalKeepsUniqueNames() {
        Goal a = newGoal("A");
        newGoal("B");
        assertThrows(DuplicateEntityException.class,
                () -> planner.updateGoal(a.getId(), "B", "", "Other", today.plusDays(30), "Not Started", 0));
        Goal renamed = planner.updateGoal(a.getId(), "A2", "x", "Health", today.plusDays(31), "On Hold", 10);
        assertEquals("A2", planner.getGoal(renamed.getId()).getName());
        assertEquals(today, planner.getGoal(a.getId()).getCreationDate());
    }

    @Test
    void deleteMissingGoalThrows() {
        assertThrows(ValidationException.class, () -> planner.deleteGoal(9999));
    }
}
