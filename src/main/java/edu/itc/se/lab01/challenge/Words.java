package edu.itc.se.lab01.challenge;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

// Tokenizer shared by WordCount and ReferenceWordCount so both split words identically
final class Words {
    private static final Pattern NON_LETTERS = Pattern.compile("[^\\p{L}]+");

    private Words() {}

    static void forEach(String line, Consumer<String> action) {
        for (String w : NON_LETTERS.split(line)) {
            if (!w.isEmpty()) action.accept(w.toLowerCase(Locale.ROOT));
        }
    }
}
