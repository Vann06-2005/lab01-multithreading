package edu.itc.se.lab01.task2;

/*
 * Wrong totals observed (8 threads × 1 000 000 increments, expected 8 000 000):
 *   run 1: 2,843,080
 *   run 2: 3,088,440
 *   run 3: 2,507,566
 * count++ is read-modify-write: two threads can read the same value and both
 * write value + 1, so one of the updates is lost.
 */
public class UnsafeCounter implements Counter {
    private long count;

    @Override
    public void increment() {
        count++;
    }

    @Override
    public long get() {
        return count;
    }
}
