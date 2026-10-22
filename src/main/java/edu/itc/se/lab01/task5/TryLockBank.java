package edu.itc.se.lab01.task5;

// Fix B: ReentrantLock per account, tryLock(10, MILLISECONDS) on both, back off and retry
public class TryLockBank {
    public static void main(String[] args) throws InterruptedException {
        // TODO: count retries; graceful shutdown via interrupt() after 2 s
    }
}
