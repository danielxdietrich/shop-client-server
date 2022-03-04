package com.pwste.shopserver;

import io.javalin.Javalin;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class ShopServer extends Application {
    private static final int PORT = 55555;
    public static final Javalin APP = Javalin.create().start(PORT);
    private final String windowTitle = "Sklep - serwer";

    @Override
    public void start(Stage stage) throws IOException {
        var fxmlLoader = new FXMLLoader(ShopServer.class.getResource("shop-server.fxml"));
        var scene = new Scene(fxmlLoader.load());
        var icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("server-icon.png")));

        stage.setTitle(windowTitle);
        stage.setScene(scene);
        stage.getIcons().add(icon);
        stage.setOnCloseRequest(event -> {
            var alert = new Alert(Alert.AlertType.CONFIRMATION, "Czy na pewno chcesz wyłączyć serwer?", ButtonType.YES, ButtonType.NO);
            var result = alert.showAndWait().orElse(ButtonType.NO);

            if (ButtonType.NO.equals(result)) {
                event.consume();
            } else APP.stop();
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}