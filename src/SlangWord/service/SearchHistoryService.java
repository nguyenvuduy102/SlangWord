package SlangWord.service;

import SlangWord.SearchHistoryEntry;
import SlangWord.SlangEntry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class SearchHistoryService {
    private final List<SearchHistoryEntry> searchHistory = new ArrayList<>();

    List<SearchHistoryEntry> getSearchHistory() {
        return new ArrayList<>(searchHistory);
    }

    int getSearchHistoryCount() {
        return searchHistory.size();
    }

    void recordSearch(String keyword, SearchHistoryEntry.Mode mode,
            List<SlangEntry> results) {
        SearchHistoryEntry entry = new SearchHistoryEntry(
                System.currentTimeMillis(), keyword.trim(), mode, results.size(),
                historyPreview(mode, results));

        searchHistory.add(entry);
    }

    private String historyPreview(SearchHistoryEntry.Mode mode, List<SlangEntry> results) {
        if (results.isEmpty()) {
            return "";
        }

        Set<String> details = new LinkedHashSet<>();
        int shown = Math.min(results.size(), 5);

        for (int i = 0; i < shown; i++) {
            SlangEntry entry = results.get(i);
            if (mode == SearchHistoryEntry.Mode.WORD) {
                details.add(String.join("; ", entry.getDefinitions()));
            } else {
                details.add(entry.getWord());
            }
        }

        String prefix = mode == SearchHistoryEntry.Mode.DEFINITION ? "Matches: " : "";
        String preview = prefix + String.join(
                mode == SearchHistoryEntry.Mode.WORD ? " | " : ", ", details);

        if (preview.length() > 300) {
            int end = Character.isHighSurrogate(preview.charAt(299)) ? 299 : 300;
            return preview.substring(0, end) + "…";
        }

        return results.size() > shown ? preview + " …" : preview;
    }
}
