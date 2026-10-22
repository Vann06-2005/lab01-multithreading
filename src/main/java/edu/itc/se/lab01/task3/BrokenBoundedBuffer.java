package edu.itc.se.lab01.task3;

import java.util.ArrayDeque;

// The README experiment: BoundedBuffer with notify() instead of notifyAll()
// and if instead of while. Run with: task3.Main --broken
public class BrokenBoundedBuffer implements OrderQueue {
    private final ArrayDeque<Order> items = new ArrayDeque<>();
    private final int capacity;

    public BrokenBoundedBuffer(int capacity) { this.capacity = capacity; }

    @Override
    public synchronized void put(Order order) throws InterruptedException {
        if (items.size() == capacity) {
            wait();
        }
        items.addLast(order);
        if (items.size() > capacity) {
            throw new IllegalStateException("buffer over capacity: " + items.size());
        }
        notify();
    }

    @Override
    public synchronized Order take() throws InterruptedException {
        if (items.isEmpty()) {
            wait();
        }
        Order order = items.removeFirst(); // throws NoSuchElementException when empty
        notify();
        return order;
    }
}
