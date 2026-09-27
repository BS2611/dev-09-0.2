package cs151.backend;

import cs151.backend.dto.CategorySummaryRow;
import cs151.backend.dto.GoalNode;
import cs151.backend.dto.GoalOverviewRow;
import cs151.backend.dto.OverallSummary;
import cs151.backend.dto.OverdueItems;
import cs151.backend.dto.SearchResults;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.Task;
import cs151.backend.persistence.DataManager;
import cs151.backend.service.GoalPlanner;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceSearchReportTest extends BackendTestBase {

    @Test
    void dataSurvivesReload() {
        Goal g = newGoal("Persistent");
        Milestone m = newMilestone(g, "M");
        Task t = newTask(m, "T");
        planner.addProgressEntry(t.getId(), "note");

        GoalPlanner reloaded = new GoalPlanner(new DataManager(dbPath));
        assertEquals("Persistent", reloaded.getGoal(g.getId()).getName());
        assertEquals(1, reloaded.getMilestonesForGoal(g.getId()).size());
        assertEquals(1, reloaded.getTasksForMilestone(m.getId()).size());
        assertEquals(1, reloaded.getProgressEntriesForTask(t.getId()).size());
    }

    @Test
    void goalDeleteCascadesToEverythingBelow() {
        Goal g = newGoal("Cascade");
        Milestone m = newMilestone(g, "M");
        Task t = newTask(m, "T");
        long entryId = planner.addProgressEntry(t.getId(), "note").getId();
        Goal other = newGoal("Other");
        Milestone otherM = newMilestone(other, "M");

        planner.deleteGoal(g.getId());

        assertTrue(dataManager.findGoalById(g.getId()).isEmpty());
        assertTrue(dataManager.findMilestoneById(m.getId()).isEmpty());
        assertTrue(dataManager.findTaskById(t.getId()).isEmpty());
        assertTrue(dataManager.findProgressEntryById(entryId).isEmpty());
        assertTrue(dataManager.findMilestoneById(otherM.getId()).isPresent());
    }

    @Test
    void milestoneAndTaskDeleteCascade() {
        Milestone m = newMilestone(newGoal("G"), "M");
        Task t = newTask(m, "T");
        long entryId = planner.addProgressEntry(t.getId(), "n").getId();

        planner.deleteTask(t.getId());
        assertTrue(dataManager.findProgressEntryById(entryId).isEmpty());

        Task t2 = newTask(m, "T2");
        planner.deleteMilestone(m.getId());
        assertTrue(dataManager.findTaskById(t2.getId()).isEmpty());
    }

    @Test
    void databaseRejectsInvalidRowsEvenIfServiceIsBypassed() {
        Goal bad = new Goal("Bad", "", "Other", today, "Completed", 50);
        assertThrows(RuntimeException.class, () -> dataManager.insertGoal(bad));
        Milestone orphan = new Milestone(777, "M", today, "Not Started");
        assertThrows(RuntimeException.class, () -> dataManager.insertMilestone(orphan));
    }

    @Test
    void searchIsCaseInsensitiveAcrossGoalsMilestonesAndTasks() {
        Goal g = planner.createGoal("Learn Guitar", "Play songs", "Personal", today.plusDays(30));
        Milestone m = planner.createMilestone(g.getId(), "Chords GUITAR basics", today.plusDays(5), null);
        planner.createTask(m.getId(), "practice guitar daily", today.plusDays(2), "High", null);
        newGoal("Unrelated");

        SearchResults r = planner.search("gUiTaR");
        assertEquals(1, r.goals().size());
        assertEquals(1, r.milestones().size());
        assertEquals(1, r.tasks().size());

        assertEquals(1, planner.search("personal").goals().size());        // category
        assertEquals(2, planner.search("NOT STARTED").goals().size());     // status
        assertEquals(1, planner.search("not started").milestones().size());
        assertEquals(1, planner.search("high").tasks().size());            // priority
        assertEquals(1, planner.search("songs").goals().size());           // description
        assertTrue(planner.search("zzz-nothing").isEmpty());
        assertTrue(planner.search("  ").isEmpty());
        assertTrue(planner.search("%").isEmpty());                         // wildcard is literal
    }

    @Test
    void reportsGiveCorrectTotalsAndSummaries() {
        Goal a = planner.createGoal("A", "", "Academic", today.plusDays(30), "In Progress", 50);
        Goal b = planner.createGoal("B", "", "Academic", today.plusDays(30), "Not Started", 10);
        Goal c = planner.createGoal("C", "", "Health", today.plusDays(30), "Completed", 100);
        planner.createGoal("D", "", "Career", today.plusDays(30), "Abandoned", 0);

        Milestone ma = newMilestone(a, "MA");
        newMilestone(a, "MA2");
        Milestone mc = newMilestone(c, "MC");
        Task t1 = newTask(ma, "T1");
        newTask(ma, "T2");
        newTask(mc, "T3");
        planner.addProgressEntry(t1.getId(), "note");

        List<GoalOverviewRow> overview = reports.goalsOverview();
        assertEquals(4, overview.size());
        GoalOverviewRow rowA = overview.stream().filter(r -> r.name().equals("A")).findFirst().orElseThrow();
        assertEquals(2, rowA.milestoneCount());
        assertEquals(2, rowA.taskCount());
        assertEquals("Academic", rowA.category());
        assertEquals(50, rowA.progress());

        Map<String, Integer> status = reports.statusSummary();
        assertEquals(1, status.get("Not Started"));
        assertEquals(1, status.get("In Progress"));
        assertEquals(0, status.get("On Hold"));
        assertEquals(1, status.get("Completed"));
        assertEquals(1, status.get("Abandoned"));

        List<CategorySummaryRow> cats = reports.categorySummary();
        assertEquals(6, cats.size());
        CategorySummaryRow academic = cats.stream().filter(r -> r.category().equals("Academic")).findFirst().orElseThrow();
        assertEquals(2, academic.goalCount());
        assertEquals(30.0, academic.averageProgress(), 0.0001);
        assertEquals(0.0, cats.stream().filter(r -> r.category().equals("Financial")).findFirst().orElseThrow()
                .averageProgress(), 0.0001);

        OverallSummary summary = reports.overallSummary();
        assertEquals(4, summary.totalGoals());
        assertEquals(1, summary.completedGoals());
        assertEquals(2, summary.activeGoals());
        assertEquals(25.0, summary.completionRate(), 0.0001);
        assertEquals(3, summary.totalTasks());

        List<GoalNode> hierarchy = reports.goalHierarchy();
        GoalNode nodeA = hierarchy.stream().filter(n -> n.goal().getName().equals("A")).findFirst().orElseThrow();
        assertEquals(2, nodeA.milestones().size());
        assertEquals(1, nodeA.milestones().stream().filter(x -> x.milestone().getMilestoneName().equals("MA"))
                .findFirst().orElseThrow().tasks().stream()
                .filter(x -> x.task().getDescription().equals("T1")).findFirst().orElseThrow()
                .progressEntries().size());

        GoalNode detail = reports.selectedGoalDetail(a.getId());
        assertEquals(2, detail.milestones().size());
        assertThrows(RuntimeException.class, () -> reports.selectedGoalDetail(9999));
        assertNotNull(b);
    }

    @Test
    void overdueItemsListOnlyUnfinishedPastDueThings() {
        Goal g = newGoal("G");
        Milestone m = planner.createMilestone(g.getId(), "M", today.plusDays(10), null);
        Task open = planner.createTask(m.getId(), "open", today.plusDays(5), null, null);
        Task done = planner.createTask(m.getId(), "done", today.plusDays(5), null, null);
        planner.markTaskCompleted(done.getId());
        Task cancelled = planner.createTask(m.getId(), "cancelled", today.plusDays(5), null, "Cancelled");
        Task future = planner.createTask(m.getId(), "future", today.plusDays(6), null, null);

        // Nothing is overdue today.
        OverdueItems none = reports.overdueItems();
        assertTrue(none.goals().isEmpty() && none.milestones().isEmpty() && none.tasks().isEmpty());

        // Move the clock-relevant dates into the past by writing directly through DataManager.
        LocalDate past = today.minusDays(3);
        Goal gPast = dataManager.findGoalById(g.getId()).orElseThrow();
        gPast.setTargetDate(past);
        dataManager.updateGoal(gPast);
        Milestone mPast = dataManager.findMilestoneById(m.getId()).orElseThrow();
        mPast.setTargetDate(past);
        dataManager.updateMilestone(mPast);
        for (Task t : List.of(open, done, cancelled)) {
            Task loaded = dataManager.findTaskById(t.getId()).orElseThrow();
            loaded.setDueDate(past);
            dataManager.updateTask(loaded);
        }

        OverdueItems overdue = reports.overdueItems();
        assertEquals(1, overdue.goals().size());
        assertEquals(1, overdue.milestones().size());
        assertEquals(List.of("open"), overdue.tasks().stream().map(Task::getDescription).toList());
        assertNotNull(future);
    }
}
