package edu.itc.se.lab01.task3;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

// Adapter so a java.util.concurrent queue can be used where OrderQueue is expected
final class BlockingOrderQueue implements OrderQueue {
    private final BlockingQueue<Order> q;

    BlockingOrderQueue(int capacity) { this.q = new ArrayBlockingQueue<>(capacity); }

    public void put(Order o) throws InterruptedException { q.put(o); }
    public Order take() throws InterruptedException { return q.take(); }
}
