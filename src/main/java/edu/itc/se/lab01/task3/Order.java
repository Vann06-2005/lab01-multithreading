package edu.itc.se.lab01.task3;

public record Order(int id, String item, int qty) {
    static final int POISON_ID = -1;

    static Order poisonPill() {
        return new Order(POISON_ID, "poison", 0);
    }

    boolean isPoison() {
        return id == POISON_ID;
    }
}
