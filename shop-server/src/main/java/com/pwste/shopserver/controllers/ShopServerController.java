package com.pwste.shopserver.controllers;

import com.pwste.shopserver.models.ShopServerModel;
import com.pwste.shopserver.models.objects.Order;
import com.pwste.shopserver.models.objects.Product;
import io.javalin.http.Context;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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

import static com.pwste.shopserver.ShopServer.APP;

public class ShopServerController implements Initializable {
    private final ShopServerModel model = new ShopServerModel();

    @FXML
    Label labelName, labelAddress, labelCost;
    @FXML
    ListView<String> listViewProducts;
    @FXML
    ListView<String> listViewOrders;
    @FXML
    ListView<String> listViewDetails;
    @FXML
    TitledPane titledPaneDetails;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        APP.get("/shop/new-order/", this::handleNewOrder);
        APP.get("/shop/get-products/", this::handleGetProducts);
        APP.get("/shop/check-availability/", this::handleCheckProductAvailability);

        listViewProducts.setItems(model.getProductsViewList());
        addContextMenus();
    }

    public void onAddProduct() {
        showAddProductDialog().ifPresent(productInfo -> {
            if (Arrays.stream(productInfo).anyMatch(String::isBlank)) return;

            var name = productInfo[0].strip();
            var cost = productInfo[1].strip();
            var amount = productInfo[2].strip();
            var product = new Product(name, Integer.parseInt(cost), Integer.parseInt(amount));

            model.addProduct(product);
        });
    }

    public void onRemoveProduct(ActionEvent e) {
        if (listViewProducts.getSelectionModel().isEmpty()) return;

        var selectedItem = listViewProducts.getSelectionModel().getSelectedItem();
        var product = model.getProductByDescription(selectedItem);

        model.removeProduct(product);
    }

    public void onRemoveOrder(ActionEvent e) {
        if (listViewOrders.getSelectionModel().isEmpty()) return;

        var selectedItem = listViewOrders.getSelectionModel().getSelectedItem();
        var order = model.getOrderByDescription(selectedItem);

        model.removeOrder(order);
        updateOrderDetails(null);
    }

    public void onChangeProductAmount(ActionEvent e) {
        if (listViewProducts.getSelectionModel().isEmpty()) return;

        var selectedItem = listViewProducts.getSelectionModel().getSelectedItem();
        var product = model.getProductByDescription(selectedItem);

        showChangeProductAmountDialog(product.amount()).ifPresent(amount -> {
            model.changeProductAmount(product, amount);
        });
    }

    public void onListViewOrdersClick() {
        if (listViewOrders.getSelectionModel().isEmpty()) return;

        var selectedItemDescription = listViewOrders.getSelectionModel().getSelectedItem();
        var order = model.getOrderByDescription(selectedItemDescription);

        updateOrderDetails(order);
    }

    public void onClose() {
        var alert = new Alert(Alert.AlertType.CONFIRMATION, "Czy na pewno chcesz wyłączyć serwer?", ButtonType.YES, ButtonType.NO);
        var result = alert.showAndWait().orElse(ButtonType.NO);

        if (ButtonType.YES.equals(result)) {
            APP.stop();
            Platform.exit();
            System.exit(0);
        }
    }

    public void onSaveOrders() {
        model.serializeOrders();
    }

    public void onLoadOrders() {
        model.deserializeOrders();

        listViewOrders.setItems(model.getOrdersViewList());
    }

    public void onSaveProducts() {
        model.serializeProducts();
    }

    public void onLoadProducts() {
        model.deserializeProducts();

        listViewProducts.setItems(model.getProductsViewList());
    }

    private void onRealizeOrder(ActionEvent e) {
        if (listViewOrders.getSelectionModel().isEmpty()) return;

        var selectedItem = listViewOrders.getSelectionModel().getSelectedItem();
        var order = model.getOrderByDescription(selectedItem);
        if (model.tryRealizeOrder(order)) {
            updateOrderDetails(null);
            var alert = new Alert(Alert.AlertType.INFORMATION, "Zamówienie zostało zrealizowane", ButtonType.OK);
            alert.showAndWait();
        } else {
            var alert = new Alert(Alert.AlertType.ERROR, "Nie wystarczająca ilość produktów", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private Optional<String[]> showAddProductDialog() {
        // 0: name, 1: cost, 2: amount
        var dialog = new Dialog<String[]>();
        dialog.setTitle("Nowy produkt");

        var addButtonType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        var cancelButtonType = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, cancelButtonType);

        var gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(15));

        var nameField = new TextField();
        var costField = new TextField();
        var amountField = new TextField();

        nameField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("[a-zA-Z0-9 ]*") ? c : null));
        costField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d*") ? c : null));
        amountField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d*") ? c : null));

        gridPane.add(new Label("Nazwa:"), 0, 0);
        gridPane.add(nameField, 0, 1);
        gridPane.add(new Label("Cena:"), 0, 2);
        gridPane.add(costField, 0, 3);
        gridPane.add(new Label("Ilość:"), 0, 4);
        gridPane.add(amountField, 0, 5);

        dialog.getDialogPane().setContent(gridPane);
        dialog.setResultConverter(dialogButton -> (dialogButton == addButtonType) ? new String[]{nameField.getText(), costField.getText(), amountField.getText()} : null);

        return dialog.showAndWait();
    }

    private Optional<Integer> showChangeProductAmountDialog(int currentAmount) {
        var dialog = new Dialog<Integer>();
        dialog.setTitle("Zmień ilość produktu");

        var changeButtonType = new ButtonType("Zmień", ButtonBar.ButtonData.OK_DONE);
        var cancelButtonType = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, cancelButtonType);

        var gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(15));

        var amountSpinner = new Spinner<Integer>(1, 100, currentAmount);

        var amountField = new TextField();
        amountField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d*") ? c : null));

        gridPane.add(new Label("Ilość:"), 0, 0);
        gridPane.add(amountSpinner, 0, 1);

        dialog.getDialogPane().setContent(gridPane);
        dialog.setResultConverter(dialogButton -> (dialogButton == changeButtonType) ? amountSpinner.getValue() : null);

        return dialog.showAndWait();
    }

    private void addContextMenus() {
        var contextMenuProducts = new ContextMenu();
        var contextMenuOrders = new ContextMenu();

        var menuItemProductRemove = new MenuItem("Usuń produkt");
        var menuItemProductChangeAmount = new MenuItem("Zmień ilość");
        var menuItemOrderDetails = new MenuItem("Pokaż szczegóły");
        var menuItemOrderRealize = new MenuItem("Zrealizuj zamówienie");
        var menuItemOrderRemove = new MenuItem("Usuń zamówienie");

        menuItemProductRemove.setOnAction(this::onRemoveProduct);
        menuItemProductChangeAmount.setOnAction(this::onChangeProductAmount);
        menuItemOrderDetails.setOnAction(e -> titledPaneDetails.setExpanded(true));
        menuItemOrderRealize.setOnAction(this::onRealizeOrder);
        menuItemOrderRemove.setOnAction(this::onRemoveOrder);

        contextMenuProducts.getItems().addAll(menuItemProductChangeAmount, menuItemProductRemove);
        contextMenuOrders.getItems().addAll(menuItemOrderDetails, menuItemOrderRealize, menuItemOrderRemove);

        listViewProducts.setContextMenu(contextMenuProducts);
        listViewOrders.setContextMenu(contextMenuOrders);
    }

    private void handleGetProducts(Context ctx) {
        model.handleGetProducts(ctx);
    }

    private void handleCheckProductAvailability(Context ctx) {
        model.handleCheckProductAvailability(ctx);
    }

    private void handleNewOrder(Context ctx) {
        model.handleNewOrder(ctx);

        listViewOrders.setItems(model.getOrdersViewList());
        listViewOrders.refresh();
    }

    private void updateOrderDetails(Order order) {
        titledPaneDetails.setExpanded(order != null);

        var name = (order == null) ? "[Nazwa]" : order.name();
        var address = (order == null) ? "[Adres]" : order.address();
        var cost = (order == null) ? "[Koszt]" : String.valueOf(order.cost());
        var products = (order == null) ? null : FXCollections.observableArrayList(order.products());

        labelName.setText(name);
        labelAddress.setText(address);
        labelCost.setText(cost + " zł");
        listViewDetails.setItems(products);
    }
}