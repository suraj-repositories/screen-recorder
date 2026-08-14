package com.oranbyte.screenrec;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class JavaFxTest extends Application {

    @Override
    public void start(Stage stage) {

        stage.setTitle("JavaFX Test");
        stage.setScene(
            new Scene(
                new Label("JavaFX is working"),
                400,
                200
            )
        );

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}