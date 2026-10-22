package edu.itc.se.lab01.challenge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// Single-threaded reference implementation used to verify WordCount
public final class ReferenceWordCount {
    private ReferenceWordCount() {}

    public static Map<String, Long> count(Path dir) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir)) {
            files = walk.filter(Files::isRegularFile).toList();
        }
        Map<String, Long> counts = new HashMap<>();
        for (Path p : files) {
            try (BufferedReader in = Files.newBufferedReader(p)) {
                String line;
                while ((line = in.readLine()) != null) {
                    Words.forEach(line, w -> counts.merge(w, 1L, Long::sum));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return counts;
    }
}
