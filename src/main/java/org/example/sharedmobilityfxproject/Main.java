package org.example.sharedmobilityfxproject;

import javafx.application.Application;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.example.sharedmobilityfxproject.controller.GameController;
import org.example.sharedmobilityfxproject.controller.MainController;
import org.example.sharedmobilityfxproject.controller.SceneController;
import org.example.sharedmobilityfxproject.model.Player;
import org.example.sharedmobilityfxproject.view.GameView;

// Main class extends Application for JavaFX application
public class Main extends Application {

    // Label to keep track of total carbon footprint
    Label carbonFootprintLabel; // Label to display carbon footprint

    /**
     * Starts the JavaFX application.
     * This method initializes the game view, scene controller, and main controller.
     * It also handles any exceptions that may occur during the initialization process.
     *
     * @param primaryStage The primary stage for this application, onto which the application scene can be set.
     * @throws Exception if any error occurs during the initialization process.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            System.out.println("Main Start");
            GameView gameView = new GameView(primaryStage);
            SceneController sceneController = new SceneController(gameView);
            MainController mainController = new MainController(sceneController, gameView);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}
