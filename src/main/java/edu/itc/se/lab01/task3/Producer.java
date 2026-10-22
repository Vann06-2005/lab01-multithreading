package edu.itc.se.lab01.task3;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable {
    private final OrderQueue queue;
    private final int firstId;
    private final int count;
    private final AtomicInteger producersLeft;
    private final int consumers;

    // Only the LAST producer to finish puts the poison pills. If every producer
    // put them, the first one done could stop all consumers while another
    // producer still has orders, which would then block forever on a full buffer.
    public Producer(OrderQueue queue, int firstId, int count, AtomicInteger producersLeft, int consumers) {
        this.queue = queue;
        this.firstId = firstId;
        this.count = count;
        this.producersLeft = producersLeft;
        this.consumers = consumers;
    }

    @Override
    public void run() {
        var rnd = ThreadLocalRandom.current();
        try {
            for (int id = firstId; id < firstId + count; id++) {
                queue.put(new Order(id, "item-" + id, rnd.nextInt(1, 6)));
                Thread.sleep(rnd.nextInt(0, 21));
            }
            if (producersLeft.decrementAndGet() == 0) {
                for (int i = 0; i < consumers; i++) {
                    queue.put(Order.poisonPill());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
