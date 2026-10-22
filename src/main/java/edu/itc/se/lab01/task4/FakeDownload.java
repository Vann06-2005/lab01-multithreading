package edu.itc.se.lab01.task4;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

public class FakeDownload implements Callable<String> {
    static final int FAIL_EVERY = 50;
    static final int SLOW_INDEX = 7;

    private final int index;
    private final String url;

    public FakeDownload(int index) {
        this.index = index;
        this.url = "https://example.com/file-" + index;
    }

    @Override
    public String call() throws Exception {
        Thread.sleep(index == SLOW_INDEX ? 5_000 : 200); // simulated network
        if (index % FAIL_EVERY == 0) {
            throw new IOException("boom");
        }
        return url + " -> " + ThreadLocalRandom.current().nextInt(1_000, 100_000);
    }
}
