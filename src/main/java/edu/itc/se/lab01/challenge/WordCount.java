package edu.itc.se.lab01.challenge;

import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class WordCount {
    private final BlockingQueue<Path> queue = new ArrayBlockingQueue<>(16);
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
    private final AtomicInteger filesDone = new AtomicInteger();
    private final LongAdder wordsDone = new LongAdder();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private static final Path POISON = Path.of("__POISON__");

    private final Path dir; private final int workers; private final boolean virtual;
    private final int top; private final long timeoutSec;

    WordCount(Path dir, int workers, boolean virtual, int top, long timeoutSec) {
        this.dir = dir;
        this.workers = workers;
        this.virtual = virtual;
        this.top = top;
        this.timeoutSec = timeoutSec;
    }

    void run() throws InterruptedException {
        ExecutorService pool = virtual ? Executors.newVirtualThreadPerTaskExecutor()
                                       : Executors.newFixedThreadPool(workers);
        Thread scanner = Thread.ofPlatform().name("scanner").start(this::scan);
        ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor();
        reporter.scheduleAtFixedRate(this::report, 500, 500, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-hook"));

        // TODO: submit 'workers' instances of this::work, arrange completion (latch / poison),
        //       enforce --timeout, then printResult(partial?) and stop everything.
    }

    private void scan() { /* Files.walk(dir) → queue.put(path); finally put one POISON per worker */ }

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
        }
    }

    private void countFile(Path p) { /* read lines, split on [^\p{L}]+, lower-case, counts.computeIfAbsent(w, k -> new LongAdder()).increment() */ }

    private void report() { /* files done / total, words, words/s, top 3 – never lock the map */ }

    void shutdown() { if (stopping.compareAndSet(false, true)) { /* interrupt scanner + workers, await ≤ 2 s, printResult(true) */ } }

    void printResult(boolean partial) { /* sort entries by count desc, print top N with %, write result.csv */ }

    public static void main(String[] args) throws Exception { /* parse args, new WordCount(...).run() */ }
}
