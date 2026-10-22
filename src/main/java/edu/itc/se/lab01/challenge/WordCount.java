package edu.itc.se.lab01.challenge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Usage: WordCount <dir> [--top N] [--timeout SECONDS] [--virtual] [--workers N]
public class WordCount {
    private final BlockingQueue<Path> queue = new ArrayBlockingQueue<>(16);
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
    private final AtomicInteger filesFound = new AtomicInteger();
    private final AtomicInteger filesDone = new AtomicInteger();
    private final LongAdder wordsDone = new LongAdder();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private static final Path POISON = Path.of("__POISON__");

    private final Path dir; private final int workers; private final boolean virtual;
    private final int top; private final long timeoutSec;

    Path resultCsv = Path.of("result.csv");
    boolean installShutdownHook = true;

    private volatile long startNanos;
    private volatile boolean partial;
    private Thread scanner;
    private ExecutorService pool;
    private ScheduledExecutorService reporter;
    private CountDownLatch workersDone;

    WordCount(Path dir, int workers, boolean virtual, int top, long timeoutSec) {
        this.dir = dir;
        this.workers = workers;
        this.virtual = virtual;
        this.top = top;
        this.timeoutSec = timeoutSec;
    }

    // Returns true when every file was counted, false when the run was cut short
    boolean run() throws InterruptedException {
        startNanos = System.nanoTime();
        workersDone = new CountDownLatch(workers);
        pool = virtual ? Executors.newVirtualThreadPerTaskExecutor()
                       : Executors.newFixedThreadPool(workers, namedThreads("worker-"));
        scanner = Thread.ofPlatform().name("scanner").start(this::scan);
        reporter = Executors.newSingleThreadScheduledExecutor(namedThreads("reporter"));
        reporter.scheduleAtFixedRate(this::report, 500, 500, TimeUnit.MILLISECONDS);
        Thread hook = new Thread(this::shutdown, "shutdown-hook");
        if (installShutdownHook) Runtime.getRuntime().addShutdownHook(hook);

        for (int i = 0; i < workers; i++) pool.submit(this::work);

        boolean finished = timeoutSec > 0
                ? workersDone.await(timeoutSec, TimeUnit.SECONDS)
                : awaitForever();
        if (finished && stopping.compareAndSet(false, true)) {
            reporter.shutdownNow();
            pool.close();
            scanner.join();
            printResult(false);
        } else if (!finished) {
            System.out.printf("[%.1fs] deadline reached – stopping%n", elapsedSec());
            shutdown();
        }
        if (installShutdownHook) {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException e) {
                // JVM is already shutting down (Ctrl+C) – the hook is running or has run
            }
        }
        return !partial;
    }

    private boolean awaitForever() throws InterruptedException {
        workersDone.await();
        return true;
    }

    private void scan() {
        try (Stream<Path> walk = Files.walk(dir)) {
            Iterator<Path> it = walk.filter(Files::isRegularFile).iterator();
            while (it.hasNext() && !Thread.currentThread().isInterrupted()) {
                Path p = it.next();
                filesFound.incrementAndGet();
                queue.put(p); // blocks when 16 paths are waiting → back-pressure
            }
            for (int i = 0; i < workers; i++) queue.put(POISON);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void work() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Path p = queue.take();
                if (p == POISON) return;
                countFile(p);
                filesDone.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            workersDone.countDown();
        }
    }

    private void countFile(Path p) {
        try (BufferedReader in = Files.newBufferedReader(p)) {
            String line;
            while ((line = in.readLine()) != null) {
                Words.forEach(line, w -> {
                    counts.computeIfAbsent(w, k -> new LongAdder()).increment();
                    wordsDone.increment();
                });
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void report() {
        double sec = elapsedSec();
        long words = wordsDone.sum();
        String top3 = topEntries(3).stream()
                .map(e -> String.format("%s(%,d)", e.getKey(), e.getValue()))
                .collect(Collectors.joining(" "));
        System.out.printf("[%.1fs] files %d/%d  words %,d  %,d w/s  top: %s%n",
                sec, filesDone.get(), filesFound.get(), words, (long) (words / sec), top3);
    }

    // Reads the map without locking it: ConcurrentHashMap iteration is weakly
    // consistent and LongAdder.sum() never blocks writers.
    private List<Map.Entry<String, Long>> topEntries(int n) {
        PriorityQueue<Map.Entry<String, Long>> heap = new PriorityQueue<>(Map.Entry.comparingByValue());
        for (Map.Entry<String, LongAdder> e : counts.entrySet()) {
            heap.add(Map.entry(e.getKey(), e.getValue().sum()));
            if (heap.size() > n) heap.poll();
        }
        List<Map.Entry<String, Long>> result = new ArrayList<>(heap);
        result.sort(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
        return result;
    }

    void shutdown() {
        if (stopping.compareAndSet(false, true)) {
            partial = true;
            reporter.shutdownNow();
            scanner.interrupt();
            pool.shutdownNow(); // interrupts the workers
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                pool.awaitTermination(2, TimeUnit.SECONDS);
                scanner.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            printResult(true);
        }
    }

    void printResult(boolean partial) {
        long total = wordsDone.sum();
        String mode = virtual ? "virtual(" + workers + ")" : "platform(" + workers + ")";
        System.out.printf("%s files=%d words=%,d elapsed=%.2fs mode=%s%n",
                partial ? "[PARTIAL]" : "DONE ", filesDone.get(), total, elapsedSec(), mode);
        System.out.printf("%5s  %-12s %10s %9s%n", "rank", "word", "count", "share");
        List<Map.Entry<String, Long>> rows = topEntries(top);
        for (int i = 0; i < rows.size(); i++) {
            var e = rows.get(i);
            System.out.printf("%5d  %-12s %,10d %7.2f %%%n", i + 1, e.getKey(), e.getValue(), share(e.getValue(), total));
        }
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(resultCsv))) {
            out.println("rank,word,count,share,partial");
            for (int i = 0; i < rows.size(); i++) {
                var e = rows.get(i);
                out.printf(Locale.ROOT, "%d,%s,%d,%.4f,%b%n", i + 1, e.getKey(), e.getValue(), share(e.getValue(), total), partial);
            }
            System.out.println(resultCsv + " written");
        } catch (IOException e) {
            System.err.println("could not write " + resultCsv + ": " + e.getMessage());
        }
    }

    Map<String, Long> snapshot() {
        Map<String, Long> copy = new HashMap<>();
        counts.forEach((w, n) -> copy.put(w, n.sum()));
        return copy;
    }

    private static double share(long count, long total) {
        return total == 0 ? 0 : 100.0 * count / total;
    }

    private double elapsedSec() {
        return (System.nanoTime() - startNanos) / 1e9;
    }

    private static ThreadFactory namedThreads(String prefix) {
        AtomicInteger n = new AtomicInteger(1);
        return r -> new Thread(r, prefix.endsWith("-") ? prefix + n.getAndIncrement() : prefix);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: WordCount <dir> [--top N] [--timeout SECONDS] [--virtual] [--workers N]");
            System.exit(2);
        }
        Path dir = Path.of(args[0]);
        int top = 10, workers = Runtime.getRuntime().availableProcessors();
        long timeout = 0;
        boolean virtual = false;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--top" -> top = Integer.parseInt(args[++i]);
                case "--timeout" -> timeout = Long.parseLong(args[++i]);
                case "--virtual" -> virtual = true;
                case "--workers" -> workers = Integer.parseInt(args[++i]);
                default -> {
                    System.err.println("unknown option: " + args[i]);
                    System.exit(2);
                }
            }
        }
        new WordCount(dir, workers, virtual, top, timeout).run();
    }
}
