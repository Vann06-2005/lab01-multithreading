package edu.itc.se.lab01.task3;

import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Runnable {
    private final OrderQueue queue;
    private final AtomicInteger processed;

    public Consumer(OrderQueue queue, AtomicInteger processed) {
        this.queue = queue;
        this.processed = processed;
    }

    @Override
    public void run() {
        // TODO: take orders, sleep 5–30 ms, print "<name> processed <order>",
        //       increment 'processed'; stop on a poison pill
    }
}
