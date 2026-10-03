package cs151.backend;

import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.Task;
import cs151.backend.persistence.DataManager;
import cs151.backend.service.GoalPlanner;
import cs151.backend.service.ReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

/** Gives every test a fresh SQLite file in a temporary folder (never the real runtime database). */
public abstract class BackendTestBase {
    @TempDir
    protected Path tempDir;

    protected Path dbPath;
    protected DataManager dataManager;
    protected GoalPlanner planner;
    protected ReportGenerator reports;

    protected final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUpBackend() {
        dbPath = tempDir.resolve("test.db");
        dataManager = new DataManager(dbPath);
        planner = new GoalPlanner(dataManager);
        reports = new ReportGenerator(dataManager);
    }

    protected Goal newGoal(String name) {
        return planner.createGoal(name, "desc", "Academic", today.plusDays(30), "Not Started", 0);
    }

    protected Milestone newMilestone(Goal goal, String name) {
        return planner.createMilestone(goal.getId(), name, today.plusDays(10), null);
    }

    protected Task newTask(Milestone milestone, String description) {
        return planner.createTask(milestone.getId(), description, today.plusDays(5), null, null);
    }
}
