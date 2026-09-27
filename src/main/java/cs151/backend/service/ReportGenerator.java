package cs151.backend.service;

import cs151.backend.dto.CategorySummaryRow;
import cs151.backend.dto.GoalNode;
import cs151.backend.dto.GoalOverviewRow;
import cs151.backend.dto.MilestoneNode;
import cs151.backend.dto.OverallSummary;
import cs151.backend.dto.OverdueItems;
import cs151.backend.dto.TaskNode;
import cs151.backend.exception.NotFoundException;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.Task;
import cs151.backend.persistence.DataManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the data for the required reports as plain Java objects (see {@code cs151.backend.dto});
 * the frontend decides how to display them.
 */
public class ReportGenerator {
    private final DataManager dataManager;

    public ReportGenerator() {
        this(new DataManager());
    }

    public ReportGenerator(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /** Report 1: category, status, progress, target date and milestone/task counts for every goal. */
    public List<GoalOverviewRow> goalsOverview() {
        List<GoalOverviewRow> rows = new ArrayList<>();
        for (Goal g : dataManager.getAllGoals()) {
            List<Milestone> milestones = dataManager.getMilestonesForGoal(g.getId());
            int tasks = 0;
            for (Milestone m : milestones) tasks += dataManager.getTasksForMilestone(m.getId()).size();
            rows.add(new GoalOverviewRow(g.getId(), g.getName(), g.getCategory(), g.getStatus(), g.getProgress(),
                    g.getTargetDate(), milestones.size(), tasks));
        }
        return rows;
    }

    /** Report 2: every goal with its milestones, tasks and progress entries. */
    public List<GoalNode> goalHierarchy() {
        List<GoalNode> nodes = new ArrayList<>();
        for (Goal g : dataManager.getAllGoals()) nodes.add(buildNode(g));
        return nodes;
    }

    /** Report 3: number of goals per status (all five statuses are present, in order, even when 0). */
    public Map<String, Integer> statusSummary() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String status : Goal.STATUSES) counts.put(status, 0);
        for (Goal g : dataManager.getAllGoals()) counts.merge(g.getStatus(), 1, Integer::sum);
        return counts;
    }

    /** Report 4: goal count and average progress for each of the six categories. */
    public List<CategorySummaryRow> categorySummary() {
        List<Goal> goals = dataManager.getAllGoals();
        List<CategorySummaryRow> rows = new ArrayList<>();
        for (String category : Goal.CATEGORIES) {
            int count = 0;
            int total = 0;
            for (Goal g : goals) {
                if (category.equals(g.getCategory())) {
                    count++;
                    total += g.getProgress();
                }
            }
            rows.add(new CategorySummaryRow(category, count, count == 0 ? 0.0 : (double) total / count));
        }
        return rows;
    }

    /** Report 5: one goal with its milestones and tasks (due dates, priorities and statuses included). */
    public GoalNode selectedGoalDetail(long goalId) {
        Goal goal = dataManager.findGoalById(goalId).orElseThrow(() -> new NotFoundException("Goal not found."));
        return buildNode(goal);
    }

    /**
     * Report 6: goals, milestones and tasks whose date is before today and that are not finished.
     * Finished means Completed/Abandoned for goals, Completed/Skipped for milestones and
     * Completed/Cancelled for tasks.
     */
    public OverdueItems overdueItems() {
        LocalDate today = LocalDate.now();
        List<Goal> goals = new ArrayList<>();
        List<Milestone> milestones = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();
        for (Goal g : dataManager.getAllGoals()) {
            if (g.getTargetDate().isBefore(today) && !Goal.STATUS_COMPLETED.equals(g.getStatus())
                    && !Goal.STATUS_ABANDONED.equals(g.getStatus())) {
                goals.add(g);
            }
            for (Milestone m : dataManager.getMilestonesForGoal(g.getId())) {
                if (m.getTargetDate().isBefore(today) && !Milestone.STATUS_COMPLETED.equals(m.getStatus())
                        && !Milestone.STATUS_SKIPPED.equals(m.getStatus())) {
                    milestones.add(m);
                }
                for (Task t : dataManager.getTasksForMilestone(m.getId())) {
                    if (t.getDueDate().isBefore(today) && !Task.STATUS_COMPLETED.equals(t.getStatus())
                            && !Task.STATUS_CANCELLED.equals(t.getStatus())) {
                        tasks.add(t);
                    }
                }
            }
        }
        return new OverdueItems(goals, milestones, tasks);
    }

    /** Report 7: totals, active goals (not Completed/Abandoned), completion rate (%) and total tasks. */
    public OverallSummary overallSummary() {
        List<Goal> goals = dataManager.getAllGoals();
        int completed = 0;
        int active = 0;
        int tasks = 0;
        for (Goal g : goals) {
            if (Goal.STATUS_COMPLETED.equals(g.getStatus())) completed++;
            else if (!Goal.STATUS_ABANDONED.equals(g.getStatus())) active++;
            for (Milestone m : dataManager.getMilestonesForGoal(g.getId())) {
                tasks += dataManager.getTasksForMilestone(m.getId()).size();
            }
        }
        double rate = goals.isEmpty() ? 0.0 : 100.0 * completed / goals.size();
        return new OverallSummary(goals.size(), completed, active, rate, tasks);
    }

    private GoalNode buildNode(Goal g) {
        List<MilestoneNode> milestones = new ArrayList<>();
        for (Milestone m : dataManager.getMilestonesForGoal(g.getId())) {
            List<TaskNode> tasks = new ArrayList<>();
            for (Task t : dataManager.getTasksForMilestone(m.getId())) {
                tasks.add(new TaskNode(t, dataManager.getProgressEntriesForTask(t.getId())));
            }
            milestones.add(new MilestoneNode(m, tasks));
        }
        return new GoalNode(g, milestones);
    }
}
