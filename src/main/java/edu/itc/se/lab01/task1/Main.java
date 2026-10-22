package edu.itc.se.lab01.task1;

/*
 * start() asks the JVM to create a NEW thread that runwws run() concurrently,
 * so the main thread keeps going ("all launched" prints right away and the
 * countdowns interleave).
 * run() is an ordinary method call: it executes on the MAIN thread, so each
 * countdown finishes before the next begins and "all launched" prints last.
 * No new thread is ever created, so getState() stays NEW.
 */
public class Main {
    static class Countdown extends Thread {
        Countdown(String name) {
            super(name);
        }

        @Override
        public void run() {
            countdown(getName());
        }
    }

    static class CountdownTask implements Runnable {
        @Override
        public void run() {
            countdown(Thread.currentThread().getName());
        }
    }

    static void countdown(String name) {
        try {
            for (int i = 3; i >= 1; i--) {
                System.out.println(name + ": " + i);
                Thread.sleep(200);
            }
            System.out.println(name + ": liftoff!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        boolean sequential = args.length > 0 && args[0].equals("--sequential");

        Thread apollo = new Countdown("apollo");
        Thread gemini = new Thread(new CountdownTask(), "gemini");
        Thread mercury = new Thread(() -> countdown(Thread.currentThread().getName()), "mercury");
        Thread artemis = Thread.ofVirtual().name("artemis").unstarted(new CountdownTask());

        Thread[] all = { apollo, gemini, mercury, artemis };

        for (Thread t : all) {
            if (sequential)
                t.run();
            else
                t.start();
        }
        System.out.println("Mission control: all launched");

        for (Thread t : all)
            t.join();
        System.out.println("Mission control: all landed");

        for (Thread t : all) {
            System.out.printf("%-8s virtual=%-5b state=%s%n", t.getName(), t.isVirtual(), t.getState());
        }
    }
}
