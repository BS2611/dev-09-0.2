package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.exception.ValidationException;
import cs151.backend.model.Goal;
import cs151.backend.service.GoalPlanner;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.time.LocalDate;

public class GoalController {

    private final GoalPlanner goalPlanner = new GoalPlanner();

    @FXML
    private TextField goalNameField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private DatePicker targetDatePicker;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TextField progressField;

    @FXML
    private TextField creationDateField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private void initialize() {
        categoryComboBox.setItems(
                FXCollections.observableArrayList(Goal.CATEGORIES)
        );

        statusComboBox.setItems(
                FXCollections.observableArrayList(Goal.STATUSES)
        );

        progressField.setText("0");
        statusComboBox.setValue(Goal.STATUS_NOT_STARTED);
        creationDateField.setText(LocalDate.now().toString());
    }

    @FXML
    private void createGoal() {
        String goalName = goalNameField.getText();
        String description = descriptionArea.getText();
        String category = categoryComboBox.getValue();
        LocalDate targetDate = targetDatePicker.getValue();
        String status = statusComboBox.getValue();

        int progress;

        try {
            progress = Integer.parseInt(progressField.getText().trim());
        } catch (NumberFormatException exception) {
            showError("Progress must be a whole number.");
            return;
        }

        try {
            goalPlanner.createGoal(
                    goalName,
                    description,
                    category,
                    targetDate,
                    status,
                    progress
            );

            feedbackLabel.setStyle("-fx-text-fill: #2E7D32;");
            feedbackLabel.setText("Goal created successfully.");

            clearForm();

        } catch (ValidationException exception) {
            showError(exception.getMessage());
        }
    }

    private void showError(String message) {
        feedbackLabel.setStyle("-fx-text-fill: #C62828;");
        feedbackLabel.setText(message);
    }

    private void clearForm() {
        goalNameField.clear();
        descriptionArea.clear();
        categoryComboBox.setValue(null);
        targetDatePicker.setValue(null);
        statusComboBox.setValue(Goal.STATUS_NOT_STARTED);
        progressField.setText("0");
        creationDateField.setText(LocalDate.now().toString());
    }

    @FXML
    private void goBack() throws IOException {
        Main.showHomePage();
    }
}