package cs151.backend.persistence;

import cs151.backend.model.Goal;

import java.util.List;
import java.util.Optional;


public interface DAL {
    long insertGoal(Goal goal);

    List<Goal> getAllGoals();

    boolean updateGoal(Goal goal);

    boolean deleteGoal(long goalId);

    Optional<Goal> findGoalById(long goalId);

    Optional<Goal> findGoalByName(String name);
}
