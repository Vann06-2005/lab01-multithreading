package edu.itc.se.lab01.task3;

// Same Producer/Consumer, but backed by ArrayBlockingQueue instead of BoundedBuffer
public class Main2 {
    public static void main(String[] args) throws InterruptedException {
        Main.run(new BlockingOrderQueue(Main.CAPACITY));
    }
}
