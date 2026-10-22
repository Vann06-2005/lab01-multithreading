package edu.itc.se.lab01.task4;

import java.util.concurrent.Callable;

public class PrimeCounter implements Callable<Long> {
    private final long from;
    private final long to;

    public PrimeCounter(long from, long to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public Long call() {
        return Main.countPrimes(from, to);
    }
}
