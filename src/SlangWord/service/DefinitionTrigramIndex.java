package SlangWord.service;

import SlangWord.SlangEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;

final class DefinitionTrigramIndex {
    static final int GRAM_SIZE = 3;

    // Mỗi khóa là ba ký tự liên tiếp trong một định nghĩa đã chuyển về chữ thường.
    // Giả sử bản ghi 12 có nghĩa "hello" và bản ghi 47 có nghĩa "help".
    // Khi đó "hel" -> [12, 47], "ell" -> [12] và "elp" -> [47].
    // TreeSet giữ ID theo thứ tự tăng dần và chỉ lưu mỗi ID một lần.
    private final Map<String, TreeSet<Long>> postings = new HashMap<>();

    void rebuild(Collection<SlangEntry> entries) {
        postings.clear();
        for (SlangEntry entry : entries) {
            add(entry);
        }
    }

    void add(SlangEntry entry) {
        Long id = entry.getId();
        forEachTrigram(entry, gram ->
                postings.computeIfAbsent(gram, key -> new TreeSet<>()).add(id));
    }

    void remove(SlangEntry entry) {
        Long id = entry.getId();
        forEachTrigram(entry, gram -> {
            TreeSet<Long> posting = postings.get(gram);
            if (posting != null) {
                posting.remove(id);
                if (posting.isEmpty()) {
                    postings.remove(gram);
                }
            }
        });
    }

    List<SlangEntry> search(String query, Map<Long, SlangEntry> entriesById) {
        List<SlangEntry> results = new ArrayList<>();

        if (query.codePointCount(0, query.length()) < GRAM_SIZE) {
            for (SlangEntry entry : entriesById.values()) {
                if (matches(entry, query)) {
                    results.add(entry);
                }
            }
            return results;
        }

        TreeSet<Long> smallest = null;
        int end = query.offsetByCodePoints(0, GRAM_SIZE);
        for (int start = 0; ; start = nextCharacter(query, start)) {
            TreeSet<Long> posting = postings.get(query.substring(start, end));
            if (posting == null) {
                return Collections.emptyList();
            }

            if (smallest == null || posting.size() < smallest.size()) {
                smallest = posting;
            }

            if (end == query.length()) {
                break;
            }
            end = nextCharacter(query, end);
        }

        // Every match contains the rarest trigram. Check the full query in a single definition.
        for (Long id : smallest) {
            SlangEntry entry = entriesById.get(id);
            if (matches(entry, query)) {
                results.add(entry);
            }
        }
        return results;
    }

    Map<String, TreeSet<Long>> getPostings() {
        return postings;
    }

    private static boolean matches(SlangEntry entry, String query) {
        for (String definition : entry.getDefinitions()) {
            if (definition.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private static void forEachTrigram(SlangEntry entry, Consumer<String> action) {
        for (String definition : entry.getDefinitions()) {
            String text = definition.toLowerCase(Locale.ROOT);
            if (text.codePointCount(0, text.length()) < GRAM_SIZE) {
                continue;
            }

            int end = text.offsetByCodePoints(0, GRAM_SIZE);
            for (int start = 0; ; start = nextCharacter(text, start)) {
                action.accept(text.substring(start, end));
                if (end == text.length()) {
                    break;
                }
                end = nextCharacter(text, end);
            }
        }
    }

    private static int nextCharacter(String text, int offset) {
        return offset + Character.charCount(text.codePointAt(offset));
    }
}
