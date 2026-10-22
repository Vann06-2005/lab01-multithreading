package edu.itc.se.lab01.task4;

import java.util.*;
import java.util.concurrent.*;

public class Main {
    static final int DOWNLOADS = 500;

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
        int cores = Runtime.getRuntime().availableProcessors();

        long t0 = System.nanoTime();
        long single = countPrimes(0, LIMIT);
        System.out.printf("single-threaded: %d primes in %d ms%n", single, (System.nanoTime() - t0) / 1_000_000);

        t0 = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(cores)) {
            List<Future<Long>> futures = new ArrayList<>();
            long chunk = LIMIT / CHUNKS;
            for (long i = 0; i < CHUNKS; i++) {
                long from = i * chunk;
                long to = (i == CHUNKS - 1) ? LIMIT : from + chunk;
                futures.add(pool.submit(new PrimeCounter(from, to)));
            }
            long total = 0;
            for (Future<Long> f : futures) total += f.get();
            System.out.printf("pool (%d threads): %d primes in %d ms%n", cores, total, (System.nanoTime() - t0) / 1_000_000);
            if (total != single) throw new IllegalStateException("parallel count differs from single-threaded");
        }

        try (ExecutorService platform = Executors.newFixedThreadPool(20)) {
            downloads("downloads on fixed(20) platform threads", platform);
        }
        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            downloads("downloads on virtual threads           ", virtual);
        }
        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            completableDownloads("CompletableFuture on virtual threads   ", virtual);
        }
    }

    static void downloads(String label, ExecutorService executor) throws InterruptedException {
        long t0 = System.nanoTime();
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 1; i <= DOWNLOADS; i++) {
            futures.add(executor.submit(new FakeDownload(i)));
        }
        int ok = 0, failed = 0, timedOut = 0;
        for (Future<String> f : futures) {
            try {
                f.get(1, TimeUnit.SECONDS);
                ok++;
            } catch (ExecutionException e) {
                failed++;
            } catch (TimeoutException e) {
                f.cancel(true);
                timedOut++;
            }
        }
        System.out.printf("%s: ok=%d failed=%d timedOut=%d in %d ms%n",
                label, ok, failed, timedOut, (System.nanoTime() - t0) / 1_000_000);
    }

    // Bonus: same downloads with CompletableFuture; handle() turns failures and
    // timeouts into fallback strings instead of exceptions.
    static void completableDownloads(String label, ExecutorService executor) {
        long t0 = System.nanoTime();
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 1; i <= DOWNLOADS; i++) {
            FakeDownload download = new FakeDownload(i);
            futures.add(CompletableFuture.supplyAsync(() -> {
                        try {
                            return download.call();
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, executor)
                    .orTimeout(1, TimeUnit.SECONDS)
                    .handle((result, error) -> {
                        if (error == null) return result;
                        Throwable cause = error instanceof CompletionException ? error.getCause() : error;
                        return cause instanceof TimeoutException ? "TIMEOUT" : "FAILED";
                    }));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        long ok = futures.stream().map(CompletableFuture::join).filter(s -> !s.equals("FAILED") && !s.equals("TIMEOUT")).count();
        long failed = futures.stream().map(CompletableFuture::join).filter(s -> s.equals("FAILED")).count();
        long timedOut = futures.stream().map(CompletableFuture::join).filter(s -> s.equals("TIMEOUT")).count();
        System.out.printf("%s: ok=%d failed=%d timedOut=%d in %d ms%n",
                label, ok, failed, timedOut, (System.nanoTime() - t0) / 1_000_000);
    }
}
