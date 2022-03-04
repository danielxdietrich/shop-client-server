package com.pwste.shopserver.models.objects;

import java.io.Serializable;

public record Product(String name, int cost, int amount) implements Serializable {
    @Override
    public String toString() {
        return String.format("%dx %s, %dzł", amount, name, cost);
    }
}
