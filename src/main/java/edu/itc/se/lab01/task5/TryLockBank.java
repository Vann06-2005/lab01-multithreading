package edu.itc.se.lab01.task5;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

// Fix B: never wait forever. If the second lock isn't free within 10 ms, release
// the first one, back off for a random moment and retry → no hold-and-wait.
// Also shows graceful shutdown: workers run until interrupted.
public class TryLockBank {
    static final int ACCOUNTS = 4;
    static final int WORKERS = 8;
    static final int TRANSFERS_PER_WORKER = 100_000;

    static final class Account {
        final int id; private long balance;
        final ReentrantLock lock = new ReentrantLock();
        Account(int id, long balance) { this.id = id; this.balance = balance; }
        long balance() { return balance; }
        void withdraw(long a) { balance -= a; }
        void deposit(long a)  { balance += a; }
    }

    private final Account[] accounts = new Account[ACCOUNTS];
    private final LongAdder retries = new LongAdder();

    TryLockBank() {
        for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);
    }

    void transfer(Account from, Account to, long amount) throws InterruptedException {
        var rnd = ThreadLocalRandom.current();
        while (true) {
            if (from.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
                            if (from.balance() >= amount) { from.withdraw(amount); to.deposit(amount); }
                            return;
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock();
                }
            }
            retries.increment();
            Thread.sleep(Duration.ofNanos(rnd.nextLong(100_000, 2_000_000)));
        }
    }

    void randomTransfer() throws InterruptedException {
        var rnd = ThreadLocalRandom.current();
        Account a = accounts[rnd.nextInt(accounts.length)];
        Account b = accounts[rnd.nextInt(accounts.length)];
        if (a == b) b = accounts[(a.id + 1) % accounts.length];
        transfer(a, b, rnd.nextInt(1, 50));
    }

    long total() {
        long total = 0;
        for (Account a : accounts) total += a.balance();
        return total;
    }

    // Fixed amount of work: 8 workers × 100 000 transfers
    void runFixed() throws InterruptedException {
        LongAdder transfers = new LongAdder();
        long start = System.nanoTime();
        Thread[] workers = new Thread[WORKERS];
        for (int w = 0; w < workers.length; w++) {
            workers[w] = new Thread(() -> {
                try {
                    for (int i = 0; i < TRANSFERS_PER_WORKER; i++) {
                        randomTransfer();
                        transfers.increment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "transfer-" + w);
            workers[w].start();
        }
        for (Thread t : workers) t.join();
        System.out.printf("transfers=%d retries=%d total=%d (expected %d) in %d ms%n",
                transfers.sum(), retries.sum(), total(), ACCOUNTS * 1_000L, (System.nanoTime() - start) / 1_000_000);
    }

    // Workers loop until interrupted; main stops them after a fixed time window
    void runUntilInterrupted(long runMillis) throws InterruptedException {
        Thread[] workers = new Thread[WORKERS];
        long[] done = new long[WORKERS];
        for (int w = 0; w < workers.length; w++) {
            int id = w;
            workers[w] = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        randomTransfer();
                        done[id]++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // restore the flag, then fall out of the loop
                }
            }, "transfer-" + w);
            workers[w].start();
        }

        Thread.sleep(runMillis);
        System.out.printf("[main] interrupting %d workers after %d ms%n", workers.length, runMillis);
        for (Thread t : workers) t.interrupt();

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        boolean allStopped = true;
        for (int w = 0; w < workers.length; w++) {
            long left = Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()));
            workers[w].join(left);
            allStopped &= !workers[w].isAlive();
            // join() gives a happens-before edge, so done[w] is safe to read here
            System.out.printf("%s stopped after %d transfers%n", workers[w].getName(), done[w]);
        }
        System.out.printf("all workers stopped=%b total=%d (expected %d)%n", allStopped, total(), ACCOUNTS * 1_000L);
    }

    public static void main(String[] args) throws InterruptedException {
        new TryLockBank().runFixed();
        new TryLockBank().runUntilInterrupted(2_000);
    }
}
