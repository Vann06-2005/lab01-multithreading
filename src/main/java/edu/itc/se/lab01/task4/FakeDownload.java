package edu.itc.se.lab01.task4;

import java.util.concurrent.Callable;

public class FakeDownload implements Callable<String> {
    private final String url;

    public FakeDownload(String url) {
        this.url = url;
    }

    @Override
    public String call() throws Exception {
        // TODO: sleep 200 ms, return url + " -> " + length;
        //       every 50th throws IOException("boom"), one sleeps 5 s
        return url;
    }
}
