package edu.itc.se.lab01.task3;

public class Producer implements Runnable {
    private final OrderQueue queue;
    private final int firstId;
    private final int count;
    private final int consumers;

    public Producer(OrderQueue queue, int firstId, int count, int consumers) {
        this.queue = queue;
        this.firstId = firstId;
        this.count = count;
        this.consumers = consumers;
    }

    @Override
    public void run() {
        // TODO: put 'count' orders with a random 0–20 ms pause,
        //       then one Order.poisonPill() per consumer
    }
}
