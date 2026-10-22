package edu.itc.se.lab01.task3;

import java.util.ArrayDeque;

public class BoundedBuffer implements OrderQueue {
    private final ArrayDeque<Order> items = new ArrayDeque<>();
    private final int capacity;

    public BoundedBuffer(int capacity) { this.capacity = capacity; }

    @Override
    public synchronized void put(Order order) throws InterruptedException {
        // TODO: while full → wait(); add; notifyAll()
    }

    @Override
    public synchronized Order take() throws InterruptedException {
        // TODO: while empty → wait(); remove; notifyAll(); return
        return null;
    }
}
