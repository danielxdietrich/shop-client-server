package com.pwste.shopclient.models;

import com.pwste.shopclient.models.objects.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.pwste.shopclient.ShopClient.PORT;

public class ShopClientModel {
    private final String address = String.format("http://localhost:%d/shop/", PORT);
    private final HttpClient client = HttpClient.newHttpClient();

    private final String nameRegexGroup = "name";
    private final String costRegexGroup = "cost";
    private final String amountRegexGroup = "amount";
    private final Pattern productDescriptionPattern = Pattern.compile("(?<" + amountRegexGroup + ">[\\d]+)x (?<" + nameRegexGroup + ">.+), (?<" + costRegexGroup + ">[\\d]+)zł");

    private final ObservableList<String> productsViewList = FXCollections.observableArrayList();
    private final ObservableList<String> basketViewList = FXCollections.observableArrayList();

    private final List<Product> productsList = new ArrayList<>();
    private final List<Product> basketList = new ArrayList<>();

    private int basketCost;

    public void setProducts(String[] products) {
        clearProducts();

        for (var productString : products) {
            var match = getMatch(productString);

            var name = match.group(nameRegexGroup).strip();
            var cost = match.group(costRegexGroup).strip();
            var amount = match.group(amountRegexGroup).strip();

            var product = new Product(name, Integer.parseInt(cost), Integer.parseInt(amount));
            addToProducts(product);
        }
    }

    public ObservableList<String> getProductsViewList() {
        return productsViewList;
    }

    public void addToBasket(Product product) {
        basketList.add(product);
        basketViewList.add(product.toString());
        updateBasketCost();
    }

    public void removeFromBasket(Product product) {
        basketList.remove(product);
        basketViewList.remove(product.toString());
        updateBasketCost();
    }

    public void addToProducts(Product product) {
        productsList.add(product);
        productsViewList.add(product.toString());
    }

    public void removeFromProducts(Product product) {
        productsList.remove(product);
        productsViewList.remove(product.toString());
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

    public Product getBasketProductByDescription(String description) {
        return basketList
                .stream()
                .filter(o -> o.toString().equals(description))
                .findFirst()
                .orElse(null);
    }

    public Product getBasketProductByName(String name) {
        return basketList
                .stream()
                .filter(o -> o.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    public void clearBasket() {
        basketViewList.clear();
        basketList.clear();
        updateBasketCost();
    }

    public void clearProducts() {
        productsViewList.clear();
        productsList.clear();
    }

    public ObservableList<String> getBasketViewList() {
        return basketViewList;
    }

    public int getBasketCost() {
        return basketCost;
    }

    public HttpResponse<String> sendRequest(String path) {
        try {
            var httpRequest = newRequest(path);
            return client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean canEstablishConnection() {
        try {
            var httpRequest = newRequest("");
            client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return false;
        }
        return true;
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

    private HttpRequest newRequest(String path) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(address + path))
                .GET()
                .build();
    }

    private void updateBasketCost() {
        basketCost = 0;
        for (var product : basketList) {
            basketCost += product.cost() * product.amount();
        }
    }

    private Matcher getMatch(String productDescription) {
        var matcher = productDescriptionPattern.matcher(productDescription);
        matcher.find();
        return matcher;
    }
}
