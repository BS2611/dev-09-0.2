package cs151.frontend.controller;

import cs151.application.Main;
import javafx.fxml.FXML;

import java.io.IOException;

public class HomeController {

    @FXML
    private void openGoalPage() throws IOException {
        Main.showGoalPage();
    }

    @FXML
    private void openGoalListPage() throws IOException {
        Main.showGoalListPage();
    }
}