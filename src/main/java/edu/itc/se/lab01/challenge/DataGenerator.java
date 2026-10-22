package edu.itc.se.lab01.challenge;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

// Writes test files of random words from a fixed 2 000-word vocabulary.
// Words follow a Zipf-like distribution so the top words look like real text.
// Usage: DataGenerator <dir> [files=200] [wordsPerFile=50000]
public final class DataGenerator {
    static final int VOCABULARY_SIZE = 2_000;
    private static final String[] COMMON = {
            "the", "and", "of", "to", "a", "in", "is", "that", "it", "was", "for", "on", "are", "as",
            "with", "his", "they", "at", "be", "this", "from", "have", "or", "by", "one", "had", "not",
            "but", "what", "all", "were", "when", "we", "there", "can", "an", "your", "which", "their"
    };
    private static final String[] SYLLABLES = {
            "ka", "ri", "to", "mel", "san", "do", "vi", "ne", "lo", "pra", "ste", "qu", "ar", "bo",
            "chi", "em", "fu", "gra", "hal", "in", "jo", "ki", "lum", "mo", "nor", "os", "pe", "ra"
    };

    private DataGenerator() {}

    static List<String> vocabulary() {
        Set<String> words = new LinkedHashSet<>(Arrays.asList(COMMON));
        Random rnd = new Random(42);
        while (words.size() < VOCABULARY_SIZE) {
            StringBuilder w = new StringBuilder();
            int parts = 2 + rnd.nextInt(3);
            for (int i = 0; i < parts; i++) w.append(SYLLABLES[rnd.nextInt(SYLLABLES.length)]);
            words.add(w.toString());
        }
        return new ArrayList<>(words);
    }

    public static void generate(Path dir, int files, int wordsPerFile, long seed) throws IOException {
        List<String> vocab = vocabulary();
        double[] cumulative = new double[vocab.size()];
        double sum = 0;
        for (int i = 0; i < cumulative.length; i++) {
            sum += 1.0 / (i + 1);
            cumulative[i] = sum;
        }
        Files.createDirectories(dir);
        Random rnd = new Random(seed);
        for (int f = 0; f < files; f++) {
            try (BufferedWriter out = Files.newBufferedWriter(dir.resolve(String.format("file-%05d.txt", f)))) {
                for (int i = 0; i < wordsPerFile; i++) {
                    int idx = Arrays.binarySearch(cumulative, rnd.nextDouble() * sum);
                    out.write(vocab.get(idx >= 0 ? idx : -idx - 1));
                    out.write((i + 1) % 12 == 0 ? "\n" : (rnd.nextInt(10) == 0 ? ", " : " "));
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: DataGenerator <dir> [files] [wordsPerFile]");
            System.exit(2);
        }
        int files = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        int words = args.length > 2 ? Integer.parseInt(args[2]) : 50_000;
        generate(Path.of(args[0]), files, words, 1);
        System.out.printf("wrote %d files × %,d words to %s%n", files, words, args[0]);
    }
}
