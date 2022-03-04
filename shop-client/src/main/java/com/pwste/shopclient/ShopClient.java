package com.pwste.shopclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class ShopClient extends Application {
    public static final int PORT = 55555;
    private final String windowTitle = "Sklep - klient";

    @Override
    public void start(Stage stage) throws IOException {
        var fxmlLoader = new FXMLLoader(ShopClient.class.getResource("shop-client.fxml"));
        var scene = new Scene(fxmlLoader.load());
        var icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("client-icon.png")));

        stage.setTitle(windowTitle);
        stage.setScene(scene);
        stage.getIcons().add(icon);
        stage.setOnCloseRequest(event -> {
            var alert = new Alert(Alert.AlertType.CONFIRMATION, "Czy na pewno chcesz wyłączyć aplikację?", ButtonType.YES, ButtonType.NO);
            var result = alert.showAndWait().orElse(ButtonType.NO);

            if (ButtonType.NO.equals(result)) {
                event.consume();
            }
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}