package com.pwste.shopserver.models;

import com.pwste.shopserver.models.objects.Order;
import com.pwste.shopserver.models.objects.Product;
import io.javalin.http.Context;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShopServerModel {
    private final String nameRegexGroup = "name";
    private final String costRegexGroup = "cost";
    private final String amountRegexGroup = "amount";
    private final Pattern productDescriptionPattern = Pattern.compile("(?<" + amountRegexGroup + ">[\\d]+)x (?<" + nameRegexGroup + ">.+), (?<" + costRegexGroup + ">[\\d]+)zł");

    private final ObservableList<String> productsViewList = FXCollections.observableArrayList();
    private final ObservableList<String> ordersViewList = FXCollections.observableArrayList();

    private List<Order> ordersList = new ArrayList<>();
    private List<Product> productsList = new ArrayList<>();

    public void addOrder(Order order) {
        ordersList.add(order);
        ordersViewList.add(order.toString());
    }

    public void removeOrder(Order order) {
        ordersList.remove(order);
        ordersViewList.remove(order.toString());
    }

    public void addProduct(Product product) {
        productsList.add(product);
        productsViewList.add(product.toString());
    }

    public void removeProduct(Product product) {
        productsList.remove(product);
        productsViewList.remove(product.toString());
    }

    public void changeProductAmount(Product product, int newAmount) {
        var newProduct = new Product(product.name(), product.cost(), newAmount);
        removeProduct(product);
        addProduct(newProduct);
    }

    public void handleNewOrder(Context ctx) {
        var req = ctx.req;
        var name = unpackString(req.getParameter("name"));
        var address = unpackString(req.getParameter("address"));
        var cost = unpackString(req.getParameter("cost"));
        var items = unpackStringArray(req.getParameter("products"));

        var newOrder = new Order(name, address, Integer.parseInt(cost), items);
        addOrder(newOrder);
    }

    public void handleGetProducts(Context ctx) {
        var packedProducts = packStringArray(productsViewList);
        ctx.result(packedProducts);
    }

    public void handleCheckProductAvailability(Context ctx) {
        var req = ctx.req;
        var name = unpackString(req.getParameter("name"));
        var address = unpackString(req.getParameter("address"));
        var cost = unpackString(req.getParameter("cost"));
        var items = unpackStringArray(req.getParameter("products"));

        var newOrder = new Order(name, address, Integer.parseInt(cost), items);
        var orderProducts = getProductsFromOrder(newOrder);
        var areAvailable = true;

        for (var orderedProduct : orderProducts) {
            if (productsList.stream().noneMatch(product -> product.name().equals(orderedProduct.name()) && product.amount() >= orderedProduct.amount())) {
                areAvailable = false;
            }
        }

        ctx.result(areAvailable ? "true" : "false");
    }

    public Order getOrderByDescription(String description) {
        return ordersList
                .stream()
                .filter(o -> o.toString().equals(description))
                .findFirst()
                .orElse(null);
    }

    public Product getProductByDescription(String description) {
        return productsList
                .stream()
                .filter(o -> o.toString().equals(description))
                .findFirst()
                .orElse(null);
    }

    public Product getProductByName(String name) {
        return productsList
                .stream()
                .filter(o -> o.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    public void serializeOrders() {
        try {
            var fileOutputStream = new FileOutputStream("OrderList");
            var objectOutputStream = new ObjectOutputStream(fileOutputStream);

            objectOutputStream.writeObject(ordersList);

            objectOutputStream.close();
            objectOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deserializeOrders() {
        try {
            var fileInputStream = new FileInputStream("OrderList");
            var objectInputStream = new ObjectInputStream(fileInputStream);

            ordersList = (ArrayList<Order>) objectInputStream.readObject();
            reloadOrderList();

            fileInputStream.close();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void serializeProducts() {
        try {
            var fileOutputStream = new FileOutputStream("ProductList");
            var objectOutputStream = new ObjectOutputStream(fileOutputStream);

            objectOutputStream.writeObject(productsList);

            objectOutputStream.close();
            objectOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deserializeProducts() {
        try {
            var fileInputStream = new FileInputStream("ProductList");
            var objectInputStream = new ObjectInputStream(fileInputStream);

            productsList = (ArrayList<Product>) objectInputStream.readObject();
            reloadProductList();

            fileInputStream.close();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public ObservableList<String> getProductsViewList() {
        return productsViewList;
    }

    public ObservableList<String> getOrdersViewList() {
        return ordersViewList;
    }

    public boolean tryRealizeOrder(Order order) {
        var orderProducts = getProductsFromOrder(order);
        if (!areProductsAvailable(orderProducts)) return false;

        for (var orderProduct : orderProducts) {
            var availableProduct = getProductByName(orderProduct.name());

            removeProduct(availableProduct);
            if (orderProduct.amount() != availableProduct.amount()) {
                var newProduct = new Product(availableProduct.name(), availableProduct.cost(), availableProduct.amount() - orderProduct.amount());
                addProduct(newProduct);
            }
        }
        removeOrder(order);
        return true;
    }

    private boolean areProductsAvailable(List<Product> products) {
        for (var product : products) {
            if (productsList.stream().noneMatch(p -> p.name().equals(product.name()))) {
                // Product is not available
                return false;
            } else if (getProductByName(product.name()).amount() < product.amount()) {
                // Amount of product in order exceeds available amount
                return false;
            }
        }
        return true;
    }

    private List<Product> getProductsFromOrder(Order order) {
        var productsList = new ArrayList<Product>();
        for (var productString : order.products()) {
            var match = getMatch(productString);

            var name = match.group(nameRegexGroup).strip();
            var cost = match.group(costRegexGroup).strip();
            var amount = match.group(amountRegexGroup).strip();

            var product = new Product(name, Integer.parseInt(cost), Integer.parseInt(amount));
            productsList.add(product);
        }
        return productsList;
    }

    private void reloadOrderList() {
        ordersViewList.clear();
        ordersList.stream().map(Order::toString).forEach(ordersViewList::add);
    }

    private void reloadProductList() {
        productsViewList.clear();
        productsList.stream().map(Product::toString).forEach(productsViewList::add);
    }

    public String packString(String str) {
        return str.strip().replaceAll(" ", "_");
    }

    public String unpackString(String str) {
        return str.replaceAll("_", " ");
    }

    public String packStringArray(List<String> strList) {
        return packString(String.join(";", strList));
    }

    public String[] unpackStringArray(String str) {
        return unpackString(str).split(";");
    }


    private Matcher getMatch(String productDescription) {
        var matcher = productDescriptionPattern.matcher(productDescription);
        matcher.find();
        return matcher;
    }
}