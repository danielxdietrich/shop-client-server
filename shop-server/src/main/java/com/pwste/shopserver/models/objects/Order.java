package com.pwste.shopserver.models.objects;

import java.io.Serializable;

public record Order(String name, String address, int cost, String[] products) implements Serializable {
    @Override
    public String toString() {
        return String.format("%s, %dzł", name, cost);
    }
}
