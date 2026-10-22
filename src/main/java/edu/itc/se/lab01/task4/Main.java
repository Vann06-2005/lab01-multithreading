package edu.itc.se.lab01.task4;

import java.util.*;
import java.util.concurrent.*;

public class Main {
    static long countPrimes(long from, long to) {
        long count = 0;
        for (long n = Math.max(from, 2); n < to; n++) {
            boolean prime = true;
            for (long d = 2; d * d <= n; d++) if (n % d == 0) { prime = false; break; }
            if (prime) count++;
        }
        return count;
    }

    public static void main(String[] args) throws Exception {
        final long LIMIT = 4_000_000, CHUNKS = 16;

        long t0 = System.nanoTime();
        long single = countPrimes(0, LIMIT);
        System.out.printf("single-threaded: %d primes in %d ms%n", single, (System.nanoTime() - t0) / 1_000_000);

        t0 = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            List<Future<Long>> futures = new ArrayList<>();
            // TODO: submit CHUNKS PrimeCounter callables covering [0, LIMIT)
            long total = 0;
            // TODO: sum futures
            System.out.printf("pool: %d primes in %d ms%n", total, (System.nanoTime() - t0) / 1_000_000);
        }

        // TODO: I/O-bound part with 500 FakeDownload tasks on platform pool vs virtual threads
        // TODO: failures every 50th task, one 5-second task with a 1-second timeout
    }
}
