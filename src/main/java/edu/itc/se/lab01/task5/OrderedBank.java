package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

// Fix A: both monitors are always taken in ascending id order, so no thread can
// hold a higher-id account while waiting for a lower one → no circular wait.
public class OrderedBank {
    static final int ACCOUNTS = 4;
    static final int WORKERS = 8;
    static final int TRANSFERS_PER_WORKER = 100_000;

    static final class Account {
        final int id; private long balance;
        Account(int id, long balance) { this.id = id; this.balance = balance; }
        long balance() { return balance; }
        void withdraw(long a) { balance -= a; }
        void deposit(long a)  { balance += a; }
    }

    static void transfer(Account from, Account to, long amount) {
        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;
        synchronized (first) {
            synchronized (second) {
                if (from.balance() >= amount) { from.withdraw(amount); to.deposit(amount); }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Account[] accounts = new Account[ACCOUNTS];
        for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);
        Bank.startWatchdog();

        LongAdder transfers = new LongAdder();
        long start = System.nanoTime();
        Thread[] workers = new Thread[WORKERS];
        for (int w = 0; w < workers.length; w++) {
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                for (int i = 0; i < TRANSFERS_PER_WORKER; i++) {
                    Account a = accounts[rnd.nextInt(accounts.length)];
                    Account b = accounts[rnd.nextInt(accounts.length)];
                    if (a == b) b = accounts[(a.id + 1) % accounts.length];
                    transfer(a, b, rnd.nextInt(1, 50));
                    transfers.increment();
                }
            }, "transfer-" + w);
            workers[w].start();
        }
        for (Thread t : workers) t.join();

        long total = 0;
        for (Account a : accounts) total += a.balance();
        System.out.printf("transfers=%d total=%d (expected %d) in %d ms%n",
                transfers.sum(), total, ACCOUNTS * 1_000L, (System.nanoTime() - start) / 1_000_000);
    }
}
