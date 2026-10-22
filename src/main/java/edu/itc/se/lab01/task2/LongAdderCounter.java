package edu.itc.se.lab01.task2;

import java.util.concurrent.atomic.LongAdder;

/*
 * Faster under contention: AtomicLong makes all 8 threads CAS the same memory
 * word, so most CAS attempts fail and the cache line bounces between cores.
 * LongAdder spreads increments over several cells (roughly one per contending
 * thread) and only adds them up in sum(), so threads rarely collide.
 * The price is that sum() is not an atomic snapshot while writers are active.
 */
public class LongAdderCounter implements Counter {
    private final LongAdder count = new LongAdder();

    @Override
    public void increment() {
        count.increment();
    }

    @Override
    public long get() {
        return count.sum();
    }
}
