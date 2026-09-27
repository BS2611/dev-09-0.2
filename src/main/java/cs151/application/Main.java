package cs151.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {


        scene = new Scene(loadView("home-view.fxml"),700, 650);

        stage.setTitle("Personal Goal Planner");
        stage.setScene(scene);
        stage.show();
    }

    public static void showHomePage() throws IOException {
        scene.setRoot(loadView("home-view.fxml"));
    }

    public static void showGoalPage() throws IOException {
        scene.setRoot(loadView("goal-view.fxml"));
    }

    private static Parent loadView(String fileName) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Main.class.getResource("/cs151/frontend/view/" + fileName)
        );
        return loader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}