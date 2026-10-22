package edu.itc.se.lab01.task3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    static final int PRODUCERS = 2;
    static final int ORDERS_PER_PRODUCER = 50;
    static final int CONSUMERS = 3;
    static final int CAPACITY = 5;

    public static void main(String[] args) throws InterruptedException {
        boolean broken = args.length > 0 && args[0].equals("--broken");
        run(broken ? new BrokenBoundedBuffer(CAPACITY) : new BoundedBuffer(CAPACITY));
    }

    // Shared by Main (hand-made buffer) and Main2 (ArrayBlockingQueue)
    static void run(OrderQueue queue) throws InterruptedException {
        long start = System.nanoTime();
        AtomicInteger processed = new AtomicInteger();
        AtomicInteger producersLeft = new AtomicInteger(PRODUCERS);
        List<Thread> threads = new ArrayList<>();

        for (int p = 0; p < PRODUCERS; p++) {
            int firstId = p * ORDERS_PER_PRODUCER + 1;
            threads.add(new Thread(new Producer(queue, firstId, ORDERS_PER_PRODUCER, producersLeft, CONSUMERS),
                    "producer-" + (p + 1)));
        }
        for (int c = 0; c < CONSUMERS; c++) {
            threads.add(new Thread(new Consumer(queue, processed), "consumer-" + (c + 1)));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        int expected = PRODUCERS * ORDERS_PER_PRODUCER;
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("Total processed: %d (expected %d) in %d ms%n", processed.get(), expected, ms);
        if (processed.get() != expected) {
            throw new IllegalStateException("lost orders: " + (expected - processed.get()));
        }
    }
}
