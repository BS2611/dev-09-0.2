package cs151.backend.persistence;

import cs151.backend.model.Goal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Stores and loads Goal objects using the data/goals.txt tile.
 */
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

            //Store one goal per line using pipe-separated fields

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
        List<Goal> goals = new ArrayList<>();

        try {
            for (String line : Files.readAllLines(filePath)) {
                if (line.isBlank()) {
                    continue;
                }

                String[] fields = line.split("\\|", -1);

                if (fields.length != 8) {
                    continue;
                }

                Goal goal = new Goal(
                        Long.parseLong(fields[0]),
                        fields[1],
                        fields[2],
                        fields[3],
                        LocalDate.parse(fields[4]),
                        fields[5],
                        Integer.parseInt(fields[6]),
                        LocalDate.parse(fields[7])
                );

                goals.add(goal);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load goals.", e);
        }

        //Return goals with the most recent creation date on the top
        goals.sort(
                Comparator.comparing(Goal::getCreationDate).reversed()
        );

        return goals;
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
        return getAllGoals()
                .stream()
                .filter(goal -> goal.getId() == goalId)
                .findFirst();
    }

    @Override
    public Optional<Goal> findGoalByName(String name) {
        return getAllGoals()
                .stream()
                .filter(goal -> goal.getName().equalsIgnoreCase(name))
                .findFirst();
    }
}
