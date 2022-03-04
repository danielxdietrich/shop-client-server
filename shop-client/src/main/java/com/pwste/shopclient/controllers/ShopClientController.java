package com.pwste.shopclient.controllers;

import com.pwste.shopclient.models.ShopClientModel;
import com.pwste.shopclient.models.objects.Product;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;

public class ShopClientController implements Initializable {
    private final ShopClientModel model = new ShopClientModel();

    @FXML
    Label labelCostTotal;
    @FXML
    ListView<String> listViewProducts, listViewBasket;
    @FXML
    Button buttonProductsRefresh, buttonOrderNew;
    @FXML
    ProgressBar progressBar;

    private static double progress;

    private enum transferDirection {TO_BASKET, TO_PRODUCTS}

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        handleConnection();
        addContextMenus();
        progressBar.setVisible(false);

        listViewBasket.setItems(model.getBasketViewList());
    }

    public void onGetProducts() {
        var result = Optional.of(ButtonType.OK);

        if (!listViewBasket.getItems().isEmpty()) {
            var alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Potwierdź pobieranie produktów");
            alert.setHeaderText("Pobieranie produktów wczyści zawartość koszyka.");
            alert.setContentText("Kontynuować?");
            result = alert.showAndWait();
        }

        if (result.isPresent() && result.get() == ButtonType.OK) {
            var getProductsResponse = model.sendRequest("get-products");
            if (getProductsResponse.body().isBlank()) return;

            var productsArray = model.unpackStringArray(getProductsResponse.body());

            new Thread(() -> {
                progressBar.setVisible(true);
                buttonProductsRefresh.setDisable(true);
                for (var i = 0.0; i < 1.0; i += 0.01) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    double finalI = i;
                    Platform.runLater(() -> progressBar.setProgress(finalI));
                }

                Platform.runLater(() -> {
                    model.setProducts(productsArray);
                    model.clearBasket();

                    listViewProducts.setItems(model.getProductsViewList());
                    listViewProducts.refresh();
                    listViewBasket.refresh();
                    progressBar.setVisible(false);
                    buttonProductsRefresh.setDisable(false);
                });
            }).start();
        }
    }

    public void onNewOrder() {
        if (listViewBasket.getItems().isEmpty()) return;

        var cost = model.getBasketCost();
        showNewOrderDialog(cost).ifPresent(order -> {
            if (Arrays.stream(order).anyMatch(String::isBlank)) return;

            var name = model.packString(order[0]);
            var address = model.packString(order[1]);
            var items = model.packStringArray(model.getBasketViewList());

            var orderRequest = String.format("new-order?name=%s&address=%s&cost=%s&products=%s", name, address, cost, items);
            var availabilityRequest = String.format("check-availability?name=%s&address=%s&cost=%s&products=%s", name, address, cost, items);

            var areAvailableResponse = model.sendRequest(availabilityRequest);
            var areAvailable = areAvailableResponse.body().equals("true");

            if (!areAvailable) {
                var alert = new Alert(Alert.AlertType.NONE, "Niektóre produkty nie są dostępne, kontynuować?", ButtonType.YES, ButtonType.NO);
                var result = alert.showAndWait().orElse(ButtonType.NO);

                if (ButtonType.NO.equals(result)) {
                    return;
                }
            }

            model.sendRequest(orderRequest);
            model.clearBasket();
        });
    }

    public void onAddToBasket(ActionEvent e) {
        fromProductsToBasket();

        refreshCostLabel();
        listViewBasket.refresh();
        listViewProducts.refresh();
    }

    public void onRemoveFromBasket(ActionEvent e) {
        fromBasketToProducts();

        refreshCostLabel();
        listViewBasket.refresh();
        listViewProducts.refresh();
    }

    public void fromProductsToBasket() {
        if (listViewProducts.getSelectionModel().isEmpty()) return;

        var selectedItem = listViewProducts.getSelectionModel().getSelectedItem();
        var product = model.getProductByDescription(selectedItem);

        var basketItem = model.getBasketProductByName(product.name());
        var productsItem = model.getProductByName(product.name());

        showMoveProductDialog(product, transferDirection.TO_BASKET).ifPresent(amountString -> {
            var amount = (int) Double.parseDouble(amountString);

            // Handle product section
            model.removeFromProducts(productsItem);
            if (product.amount() != amount) {
                var newProductsItem = new Product(product.name(), product.cost(), product.amount() - amount);
                model.addToProducts(newProductsItem);
            }

            // Handle basket section
            Product newBasketItem;
            if (basketItem != null) {
                model.removeFromBasket(basketItem);
                newBasketItem = new Product(basketItem.name(), basketItem.cost(), basketItem.amount() + amount);
            } else {
                newBasketItem = new Product(product.name(), product.cost(), amount);
            }
            model.addToBasket(newBasketItem);
        });
    }

    public void fromBasketToProducts() {
        if (listViewBasket.getSelectionModel().isEmpty()) return;

        var selectedItem = listViewBasket.getSelectionModel().getSelectedItem();
        var product = model.getBasketProductByDescription(selectedItem);

        var basketItem = model.getBasketProductByName(product.name());
        var productsItem = model.getProductByName(product.name());

        showMoveProductDialog(product, transferDirection.TO_PRODUCTS).ifPresent(amountString -> {
            var amount = (int) Double.parseDouble(amountString);

            // Handle basket section
            model.removeFromBasket(basketItem);
            if (product.amount() != amount) {
                var newBasketItem = new Product(product.name(), product.cost(), product.amount() - amount);
                model.addToBasket(newBasketItem);
            }

            // Handle product section
            Product newProductsItem;
            if (productsItem != null) {
                model.removeFromProducts(productsItem);
                newProductsItem = new Product(productsItem.name(), productsItem.cost(), productsItem.amount() + amount);
            } else {
                newProductsItem = new Product(product.name(), product.cost(), amount);
            }
            model.addToProducts(newProductsItem);
        });
    }

    public void onClose() {
        var alert = new Alert(Alert.AlertType.CONFIRMATION, "Czy na pewno chcesz wyłączyć aplikację?", ButtonType.YES, ButtonType.NO);
        var result = alert.showAndWait().orElse(ButtonType.NO);

        if (ButtonType.YES.equals(result)) {
            Platform.exit();
            System.exit(0);
        }
    }

    private void refreshCostLabel() {
        labelCostTotal.setText(String.valueOf(model.getBasketCost()));
    }

    private Optional<String> showMoveProductDialog(Product product, transferDirection direction) {
        var title = switch (direction) {
            case TO_BASKET -> "Dodaj do koszyka";
            case TO_PRODUCTS -> "Usuń z koszyka";
        };

        var buttonText = switch (direction) {
            case TO_BASKET -> "Dodaj";
            case TO_PRODUCTS -> "Usuń";
        };

        var dialog = new Dialog<String>();
        dialog.setTitle(title);

        var addButtonType = new ButtonType(buttonText, ButtonBar.ButtonData.OK_DONE);
        var cancelButtonType = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, cancelButtonType);

        var gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(15));

        var productLabel = new Label(String.format("%dx %s, %dzł", 1, product.name(), product.cost()));

        var amountSlider = new Slider();
        amountSlider.setMin(1);
        amountSlider.setMax(product.amount());
        amountSlider.setValue(1);
        amountSlider.setBlockIncrement(1);
        amountSlider.setMajorTickUnit(1);
        amountSlider.setMinorTickCount(0);
        amountSlider.setSnapToTicks(true);
        amountSlider.setShowTickMarks(true);
        amountSlider.setShowTickLabels(true);

        amountSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) newValue = 1;
            var cost = product.cost() * newValue.intValue();
            productLabel.setText(String.format("%sx %s, %dzł", newValue.intValue(), product.name(), cost));
        });

        gridPane.add(productLabel, 0, 0);
        gridPane.add(amountSlider, 0, 1);

        dialog.getDialogPane().setContent(gridPane);

        dialog.setResultConverter(dialogButton -> (dialogButton == addButtonType) ? String.valueOf(amountSlider.getValue()) : null);

        return dialog.showAndWait();
    }


    private void addContextMenus() {
        var contextMenuProducts = new ContextMenu();
        var contextMenuBasket = new ContextMenu();

        var menuItemAddToBasket = new MenuItem("Dodaj do koszyka");
        var menuItemRemoveFromBasket = new MenuItem("Usuń z koszyka");

        menuItemAddToBasket.setOnAction(this::onAddToBasket);
        menuItemRemoveFromBasket.setOnAction(this::onRemoveFromBasket);

        contextMenuProducts.getItems().add(menuItemAddToBasket);
        contextMenuBasket.getItems().add(menuItemRemoveFromBasket);

        listViewProducts.setContextMenu(contextMenuProducts);
        listViewBasket.setContextMenu(contextMenuBasket);
    }

    private Optional<String[]> showNewOrderDialog(int cost) {
        var dialog = new Dialog<String[]>();
        dialog.setTitle("Złóż zamówienie");

        var orderButtonType = new ButtonType("Zamów", ButtonBar.ButtonData.OK_DONE);
        var cancelButtonType = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(orderButtonType, cancelButtonType);

        var gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(15));

        var nameField = new TextField();
        var addressField = new TextField();

        gridPane.add(new Label("Imię i nazwisko:"), 0, 0);
        gridPane.add(nameField, 0, 1);
        gridPane.add(new Label("Adres:"), 0, 2);
        gridPane.add(addressField, 0, 3);
        gridPane.add(new Label(String.format("Cena: %d zł", cost)), 0, 4);

        dialog.getDialogPane().setContent(gridPane);
        dialog.setResultConverter(dialogButton -> (dialogButton == orderButtonType) ? new String[]{nameField.getText(), addressField.getText()} : null);

        return dialog.showAndWait();
    }

    private void handleConnection() {
        if (!model.canEstablishConnection()) {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd połączenia");
            alert.setHeaderText("Błąd połączenia z serwerem");
            alert.setContentText("Nie można nawiązać połączenia z serwerem.");
            alert.showAndWait();
            onClose();
        }
    }
}