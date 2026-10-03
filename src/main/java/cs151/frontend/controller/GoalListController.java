package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.model.Goal;
import cs151.backend.service.GoalPlanner;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GoalListController {

    @FXML
    private TableView<Goal> goalsTable;

    @FXML
    private TableColumn<Goal, String> nameColumn;

    @FXML
    private TableColumn<Goal, String> descriptionColumn;

    @FXML
    private TableColumn<Goal, String> categoryColumn;

    @FXML
    private TableColumn<Goal, String> targetDateColumn;

    @FXML
    private TableColumn<Goal, String> statusColumn;

    @FXML
    private TableColumn<Goal, Integer> progressColumn;

    @FXML
    private TableColumn<Goal, String> creationDateColumn;

    private final GoalPlanner goalPlanner = new GoalPlanner();

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getName())
        );

        descriptionColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getDescription())
        );

        categoryColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getCategory())
        );

        targetDateColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getTargetDate()))
        );

        statusColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getStatus())
        );

        progressColumn.setCellValueFactory(
                cell -> new ReadOnlyIntegerWrapper(
                        cell.getValue().getProgress()
                ).asObject()
        );

        creationDateColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getCreationDate()))
        );

        refreshGoals();
    }

    @FXML
    private void refreshGoals() {
        List<Goal> goals = new ArrayList<>(goalPlanner.getAllGoals());

        goals.sort(
                Comparator.comparing(Goal::getCreationDate).reversed()
        );

        goalsTable.setItems(
                FXCollections.observableArrayList(goals)
        );
    }

    @FXML
    private void goBack() throws IOException {
        Main.showHomePage();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }
}