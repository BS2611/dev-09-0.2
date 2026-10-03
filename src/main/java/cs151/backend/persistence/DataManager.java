package cs151.backend.persistence;

import cs151.backend.exception.DuplicateEntityException;
import cs151.backend.exception.NotFoundException;
import cs151.backend.exception.PersistenceException;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.ProgressEntry;
import cs151.backend.model.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The only class that contains SQL/JDBC. Stores everything in a SQLite file that is created (together with
 * its tables) automatically. Every connection enables foreign keys so ON DELETE CASCADE works; dates are stored
 * as ISO-8601 text (yyyy-MM-dd).
 *
 * <p>Insert methods set the generated id on the passed model object and also return it. Update/delete methods
 * return whether a row was affected. Each write runs in its own transaction and is rolled back on failure.
 */
public class DataManager implements DAL {
    /** Default runtime database location, relative to the application's working directory. */
    public static final Path DEFAULT_DB_PATH = Paths.get("data", "my-goal-tracker.db");

    private static final String SCHEMA_GOALS = """
            CREATE TABLE IF NOT EXISTS goals (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                name          TEXT    NOT NULL UNIQUE CHECK (length(trim(name)) > 0),
                description   TEXT,
                category      TEXT    NOT NULL CHECK (category IN
                                  ('Academic','Career','Health','Financial','Personal','Other')),
                target_date   TEXT    NOT NULL,
                status        TEXT    NOT NULL CHECK (status IN
                                  ('Not Started','In Progress','On Hold','Completed','Abandoned')),
                progress      INTEGER NOT NULL CHECK (progress BETWEEN 0 AND 100),
                creation_date TEXT    NOT NULL,
                CHECK (status <> 'Completed' OR progress = 100)
            )""";

    private static final String SCHEMA_MILESTONES = """
            CREATE TABLE IF NOT EXISTS milestones (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                goal_id     INTEGER NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
                name        TEXT    NOT NULL CHECK (length(trim(name)) > 0),
                target_date TEXT    NOT NULL,
                status      TEXT    NOT NULL CHECK (status IN
                                ('Not Started','In Progress','Completed','Skipped')),
                UNIQUE (goal_id, name)
            )""";

    private static final String SCHEMA_TASKS = """
            CREATE TABLE IF NOT EXISTS tasks (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                milestone_id    INTEGER NOT NULL REFERENCES milestones(id) ON DELETE CASCADE,
                description     TEXT    NOT NULL CHECK (length(trim(description)) > 0),
                due_date        TEXT    NOT NULL,
                priority        TEXT    NOT NULL CHECK (priority IN ('Low','Medium','High')),
                status          TEXT    NOT NULL CHECK (status IN
                                    ('To Do','In Progress','Completed','Cancelled')),
                completion_date TEXT,
                UNIQUE (milestone_id, description)
            )""";

    private static final String SCHEMA_PROGRESS = """
            CREATE TABLE IF NOT EXISTS progress_entries (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id    INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                entry_date TEXT    NOT NULL,
                note       TEXT    NOT NULL CHECK (length(trim(note)) > 0)
            )""";

    @FunctionalInterface
    private interface SqlFunction<T, R> {
        R apply(T t) throws SQLException;
    }

    private final String url;

    /** Uses the default database file {@code data/my-goal-tracker.db}. */
    public DataManager() {
        this(DEFAULT_DB_PATH);
    }

    /** Uses the given database file, creating its directory, the file and the tables if needed. */
    public DataManager(Path dbPath) {
        try {
            Path absolute = dbPath.toAbsolutePath();
            if (absolute.getParent() != null) {
                Files.createDirectories(absolute.getParent());
            }
            this.url = "jdbc:sqlite:" + absolute;
        } catch (IOException e) {
            throw new PersistenceException("Could not create the database folder.", e);
        }
        initializeSchema();
    }

    // ------------------------------------------------------------------ infrastructure

    /** Opens a connection with foreign-key enforcement switched on (SQLite has it off by default). */
    private Connection openConnection() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            c.close();
            throw e;
        }
        return c;
    }

    private <R> R read(SqlFunction<Connection, R> work) {
        try (Connection c = openConnection()) {
            return work.apply(c);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Runs the work in a transaction: commit on success, rollback on any failure. */
    private <R> R write(SqlFunction<Connection, R> work) {
        try (Connection c = openConnection()) {
            c.setAutoCommit(false);
            try {
                R result = work.apply(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    private RuntimeException translate(SQLException e) {
        String msg = String.valueOf(e.getMessage());
        if (msg.contains("UNIQUE constraint failed")) {
            if (msg.contains("goals.name")) return new DuplicateEntityException("Goal name already exists.");
            if (msg.contains("milestones.")) {
                return new DuplicateEntityException("Milestone name already exists for this goal.");
            }
            if (msg.contains("tasks.")) {
                return new DuplicateEntityException("Task description already exists for this milestone.");
            }
        }
        if (msg.contains("FOREIGN KEY constraint failed")) {
            return new NotFoundException("The parent record does not exist.");
        }
        return new PersistenceException("Database error: " + msg, e);
    }

    private void initializeSchema() {
        write(c -> {
            try (Statement s = c.createStatement()) {
                s.execute(SCHEMA_GOALS);
                s.execute(SCHEMA_MILESTONES);
                s.execute(SCHEMA_TASKS);
                s.execute(SCHEMA_PROGRESS);
            }
            return null;
        });
    }

    private static String str(LocalDate d) {
        return d == null ? null : d.toString();
    }

    private static LocalDate date(ResultSet rs, String column) throws SQLException {
        String s = rs.getString(column);
        return s == null ? null : LocalDate.parse(s);
    }

    private static long insertAndGetId(PreparedStatement ps) throws SQLException {
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("No generated key returned.");
            return keys.getLong(1);
        }
    }

    private <T> Optional<T> queryOne(String sql, long id, SqlFunction<ResultSet, T> mapper) {
        return read(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapper.apply(rs)) : Optional.<T>empty();
                }
            }
        });
    }

    private <T> List<T> queryList(String sql, Object param, SqlFunction<ResultSet, T> mapper) {
        return read(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                if (param != null) ps.setObject(1, param);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> list = new ArrayList<>();
                    while (rs.next()) list.add(mapper.apply(rs));
                    return list;
                }
            }
        });
    }

    private boolean deleteById(String table, long id) {
        return write(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    // ------------------------------------------------------------------ goals

    private static Goal mapGoal(ResultSet rs) throws SQLException {
        return new Goal(rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                rs.getString("category"), date(rs, "target_date"), rs.getString("status"),
                rs.getInt("progress"), date(rs, "creation_date"));
    }

    public long insertGoal(Goal goal) {
        long id = write(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO goals (name, description, category, target_date, status, progress, creation_date) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, goal.getName());
                ps.setString(2, goal.getDescription());
                ps.setString(3, goal.getCategory());
                ps.setString(4, str(goal.getTargetDate()));
                ps.setString(5, goal.getStatus());
                ps.setInt(6, goal.getProgress());
                ps.setString(7, str(goal.getCreationDate()));
                return insertAndGetId(ps);
            }
        });
        goal.setId(id);
        return id;
    }

    public boolean updateGoal(Goal goal) {
        return write(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE goals SET name = ?, description = ?, category = ?, target_date = ?, status = ?, "
                            + "progress = ? WHERE id = ?")) {
                ps.setString(1, goal.getName());
                ps.setString(2, goal.getDescription());
                ps.setString(3, goal.getCategory());
                ps.setString(4, str(goal.getTargetDate()));
                ps.setString(5, goal.getStatus());
                ps.setInt(6, goal.getProgress());
                ps.setLong(7, goal.getId());
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** Deletes the goal; SQLite cascades to its milestones, tasks and progress entries. */
    public boolean deleteGoal(long goalId) {
        return deleteById("goals", goalId);
    }

    public Optional<Goal> findGoalById(long goalId) {
        return queryOne("SELECT * FROM goals WHERE id = ?", goalId, DataManager::mapGoal);
    }

    /** Exact (case-sensitive) match on the trimmed name. */
    public Optional<Goal> findGoalByName(String name) {
        List<Goal> found = queryList("SELECT * FROM goals WHERE name = ?", name == null ? "" : name.trim(),
                DataManager::mapGoal);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    public List<Goal> getAllGoals() {
        return queryList("SELECT * FROM goals ORDER BY target_date, name", null, DataManager::mapGoal);
    }

    // ------------------------------------------------------------------ milestones

    private static Milestone mapMilestone(ResultSet rs) throws SQLException {
        return new Milestone(rs.getLong("id"), rs.getLong("goal_id"), rs.getString("name"),
                date(rs, "target_date"), rs.getString("status"));
    }

    public long insertMilestone(Milestone m) {
        long id = write(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO milestones (goal_id, name, target_date, status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, m.getGoalId());
                ps.setString(2, m.getMilestoneName());
                ps.setString(3, str(m.getTargetDate()));
                ps.setString(4, m.getStatus());
                return insertAndGetId(ps);
            }
        });
        m.setId(id);
        return id;
    }

    public boolean updateMilestone(Milestone m) {
        return write(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE milestones SET name = ?, target_date = ?, status = ? WHERE id = ?")) {
                ps.setString(1, m.getMilestoneName());
                ps.setString(2, str(m.getTargetDate()));
                ps.setString(3, m.getStatus());
                ps.setLong(4, m.getId());
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** Deletes the milestone; SQLite cascades to its tasks and their progress entries. */
    public boolean deleteMilestone(long milestoneId) {
        return deleteById("milestones", milestoneId);
    }

    public Optional<Milestone> findMilestoneById(long milestoneId) {
        return queryOne("SELECT * FROM milestones WHERE id = ?", milestoneId, DataManager::mapMilestone);
    }

    public List<Milestone> getMilestonesForGoal(long goalId) {
        return queryList("SELECT * FROM milestones WHERE goal_id = ? ORDER BY target_date, name", goalId,
                DataManager::mapMilestone);
    }

    // ------------------------------------------------------------------ tasks

    private static Task mapTask(ResultSet rs) throws SQLException {
        return new Task(rs.getLong("id"), rs.getLong("milestone_id"), rs.getString("description"),
                date(rs, "due_date"), rs.getString("priority"), rs.getString("status"),
                date(rs, "completion_date"));
    }

    public long insertTask(Task t) {
        long id = write(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO tasks (milestone_id, description, due_date, priority, status, completion_date) "
                            + "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, t.getMilestoneId());
                ps.setString(2, t.getDescription());
                ps.setString(3, str(t.getDueDate()));
                ps.setString(4, t.getPriority());
                ps.setString(5, t.getStatus());
                ps.setString(6, str(t.getCompletionDate()));
                return insertAndGetId(ps);
            }
        });
        t.setId(id);
        return id;
    }

    public boolean updateTask(Task t) {
        return write(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE tasks SET description = ?, due_date = ?, priority = ?, status = ?, "
                            + "completion_date = ? WHERE id = ?")) {
                ps.setString(1, t.getDescription());
                ps.setString(2, str(t.getDueDate()));
                ps.setString(3, t.getPriority());
                ps.setString(4, t.getStatus());
                ps.setString(5, str(t.getCompletionDate()));
                ps.setLong(6, t.getId());
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** Deletes the task; SQLite cascades to its progress entries. */
    public boolean deleteTask(long taskId) {
        return deleteById("tasks", taskId);
    }

    public Optional<Task> findTaskById(long taskId) {
        return queryOne("SELECT * FROM tasks WHERE id = ?", taskId, DataManager::mapTask);
    }

    public List<Task> getTasksForMilestone(long milestoneId) {
        return queryList("SELECT * FROM tasks WHERE milestone_id = ? ORDER BY due_date, description", milestoneId,
                DataManager::mapTask);
    }

    // ------------------------------------------------------------------ progress entries

    private static ProgressEntry mapProgress(ResultSet rs) throws SQLException {
        return new ProgressEntry(rs.getLong("id"), rs.getLong("task_id"), date(rs, "entry_date"),
                rs.getString("note"));
    }

    public long insertProgressEntry(ProgressEntry e) {
        long id = write(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO progress_entries (task_id, entry_date, note) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, e.getTaskId());
                ps.setString(2, str(e.getEntryDate()));
                ps.setString(3, e.getNote());
                return insertAndGetId(ps);
            }
        });
        e.setId(id);
        return id;
    }

    /** Only the note can change; the entry date is fixed at creation. */
    public boolean updateProgressEntry(ProgressEntry e) {
        return write(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE progress_entries SET note = ? WHERE id = ?")) {
                ps.setString(1, e.getNote());
                ps.setLong(2, e.getId());
                return ps.executeUpdate() > 0;
            }
        });
    }

    public boolean deleteProgressEntry(long entryId) {
        return deleteById("progress_entries", entryId);
    }

    public Optional<ProgressEntry> findProgressEntryById(long entryId) {
        return queryOne("SELECT * FROM progress_entries WHERE id = ?", entryId, DataManager::mapProgress);
    }

    public List<ProgressEntry> getProgressEntriesForTask(long taskId) {
        return queryList("SELECT * FROM progress_entries WHERE task_id = ? ORDER BY entry_date, id", taskId,
                DataManager::mapProgress);
    }

    // ------------------------------------------------------------------ search

    /** Turns user text into a case-insensitive "contains" LIKE pattern, escaping LIKE wildcards. */
    private static String likePattern(String query) {
        String q = query.trim().toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + q + "%";
    }

    private static String likeAny(String... columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(" OR ");
            sb.append("LOWER(COALESCE(").append(columns[i]).append(", '')) LIKE ? ESCAPE '\\'");
        }
        return sb.toString();
    }

    private <T> List<T> searchTable(String table, String[] columns, String order, String query,
                                    SqlFunction<ResultSet, T> mapper) {
        String pattern = likePattern(query);
        return read(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM " + table + " WHERE " + likeAny(columns) + " ORDER BY " + order)) {
                for (int i = 1; i <= columns.length; i++) ps.setString(i, pattern);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> list = new ArrayList<>();
                    while (rs.next()) list.add(mapper.apply(rs));
                    return list;
                }
            }
        });
    }

    /** Goals whose name, description, category or status contains the text (case-insensitive). */
    public List<Goal> searchGoals(String query) {
        return searchTable("goals", new String[]{"name", "description", "category", "status"},
                "target_date, name", query, DataManager::mapGoal);
    }

    /** Milestones whose name or status contains the text (case-insensitive). */
    public List<Milestone> searchMilestones(String query) {
        return searchTable("milestones", new String[]{"name", "status"}, "target_date, name", query,
                DataManager::mapMilestone);
    }

    /** Tasks whose description, priority or status contains the text (case-insensitive). */
    public List<Task> searchTasks(String query) {
        return searchTable("tasks", new String[]{"description", "priority", "status"}, "due_date, description",
                query, DataManager::mapTask);
    }
}
