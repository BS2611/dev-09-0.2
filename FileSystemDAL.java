package cs151.backend.persistence;

import cs151.backend.model.Goal;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class FileSystemDAL implements DAL {

    private final Path filePath = Paths.get("data", "goals.txt");

    public FileSystemDAL() {
        try {
            Files.createDirectories(filePath.getParent());

            if (Files.notExists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create goals file.", e);
        }
    }

    @Override
    public long insertGoal(Goal goal) {
        try {
            List<String> lines = Files.readAllLines(filePath);

            long id = lines.size() + 1;
            goal.setId(id);

            String record = goal.getId() + "|" +
                    goal.getName() + "|" +
                    goal.getDescription() + "|" +
                    goal.getCategory() + "|" +
                    goal.getTargetDate() + "|" +
                    goal.getStatus() + "|" +
                    goal.getProgress() + "|" +
                    goal.getCreationDate();

            lines.add(record);
            Files.write(filePath, lines);

            return id;
        } catch (IOException e) {
            throw new RuntimeException("Could not save goal.", e);
        }
    }

    @Override
    public List<Goal> getAllGoals() {
        return new ArrayList<>();
    }

    @Override
    public boolean updateGoal(Goal goal) {
        return false;
    }

    @Override
    public boolean deleteGoal(long goalId) {
        return false;
    }

    @Override
    public Optional<Goal> findGoalById(long goalId) {
        return Optional.empty();
    }

    @Override
    public Optional<Goal> findGoalByName(String name) {
        return Optional.empty();
    }
}
