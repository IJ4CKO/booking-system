package com.bookingapp;

import com.bookingapp.dao.DatabaseInitialiser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the booking and inventory management system.
 *
 * This class starts the JavaFX application, initialises the database and loads
 * the login screen when the programme is launched.
 */
public class Main extends Application {

    /**
     * Starts the JavaFX application.
     *
     * This method is called automatically by JavaFX when the application is
     * launched. It initialises the database, loads the login FXML file, creates
     * the main scene and displays the application window.
     *
     * @param stage the main application window provided by JavaFX
     * @throws Exception if the database or login screen cannot be loaded
     */
    @Override
    public void start(Stage stage) throws Exception {
        DatabaseInitialiser.initialise();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/bookingapp/ui/login.fxml"
                )
        );

        Scene scene = new Scene(loader.load());

        stage.setTitle(
                "Booking and Inventory Management System"
        );

        stage.setScene(scene);

        /*
         * The login page is maximised only when the application is initially
         * launched.
         *
         * Controllers only replace the scene afterwards, so the user's current
         * window size and position are preserved.
         */
        stage.setMaximized(true);
        stage.show();
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments passed to the programme
     */
    public static void main(String[] args) {
        launch(args);
    }
}