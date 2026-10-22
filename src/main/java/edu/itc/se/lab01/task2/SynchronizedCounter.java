package edu.itc.se.lab01.task2;

public class SynchronizedCounter implements Counter {
    private long count;

    @Override
    public synchronized void increment() {
        count++;
    }

    // Also synchronized: without it a reader has no happens-before edge with the
    // writers and may see a stale (or, for a long, even torn) value.
    @Override
    public synchronized long get() {
        return count;
    }
}
