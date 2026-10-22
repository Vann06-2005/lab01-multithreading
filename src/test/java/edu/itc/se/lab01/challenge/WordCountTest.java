package edu.itc.se.lab01.challenge;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WordCountTest {

    @TempDir
    Path tmp;

    private WordCount newWordCount(Path data, int workers, boolean virtual, long timeoutSec) {
        WordCount wc = new WordCount(data, workers, virtual, 5, timeoutSec);
        wc.resultCsv = tmp.resolve("result.csv");
        wc.installShutdownHook = false;
        return wc;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void parallelMatchesSingleThreaded(boolean virtual) throws Exception {
        Path data = tmp.resolve("fixture");
        DataGenerator.generate(data, 20, 5_000, 7);

        Map<String, Long> expected = ReferenceWordCount.count(data);
        WordCount wc = newWordCount(data, 4, virtual, 0);

        assertTrue(wc.run(), "run should complete");
        assertEquals(expected, wc.snapshot());
    }

    @Test
    void stopsWithinTimeout() throws Exception {
        Path data = tmp.resolve("many");
        DataGenerator.generate(data, 5_000, 6_000, 11);

        WordCount wc = newWordCount(data, 1, false, 1);
        long start = System.nanoTime();
        boolean completed = wc.run();
        double seconds = (System.nanoTime() - start) / 1e9;

        assertFalse(completed, "a 1 s timeout on 5 000 files should give a partial result");
        assertTrue(seconds < 3, "stopped after " + seconds + " s, expected < 3 s");
    }
}
