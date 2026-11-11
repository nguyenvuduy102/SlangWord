package SlangWord.service;

import SlangWord.QuizQuestion;
import SlangWord.SearchHistoryEntry;
import SlangWord.SlangEntry;
import SlangWord.io.LoadResult;
import SlangWord.io.SlangFileReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DictionaryService {
    // Mỗi ID trỏ tới một bản ghi đầy đủ gồm slang và danh sách nghĩa.
    // Ví dụ, ID 12 là LOL với nghĩa "Laugh out loud", ID 47 là LOL với nghĩa "Lots of love".
    // <12, LOL` "Laugh out loud">
    //<47, LOL ` "Lots of love">
    private final Map<Long, SlangEntry> entriesById = new HashMap<>();

    // Khóa slang đã chuẩn hóa trỏ tới tập ID của các bản ghi cùng từ.
    // Ví dụ, "lol" -> [12, 47]. Khi tra LOL, lấy hai bản ghi theo ID từ entriesById.
    //<"lol", [12, 47]>
    private final Map<String, Set<Long>> wordIndex = new HashMap<>();
    

    private DefinitionTrigramIndex definitionIndex = new DefinitionTrigramIndex();

    private final SearchHistoryService history = new SearchHistoryService();
    private final QuizService quiz = new QuizService(entriesById, wordIndex);

    private DictionaryService() {
    }

    public DictionaryService(List<SlangEntry> entries) {
        for (SlangEntry entry : entries) {
            long id = entry.getId();
            if (entriesById.putIfAbsent(id, entry) != null) {
                throw new IllegalArgumentException("Duplicate entry ID: " + id);
            }

            String key = normalize(entry.getWord());
            wordIndex.computeIfAbsent(key, word -> new LinkedHashSet<>()).add(id);
        }
        definitionIndex.rebuild(entriesById.values());
    }

    public List<SearchHistoryEntry> getSearchHistory() {
        return history.getSearchHistory();
    }

    public int getSearchHistoryCount() {
        return history.getSearchHistoryCount();
    }


    public List<SlangEntry> searchByWord(String keyword) {
        List<SlangEntry> results = findByWord(keyword);

        if (keyword != null && !keyword.trim().isEmpty()) {
            history.recordSearch(keyword, SearchHistoryEntry.Mode.WORD, results);
        }

        return results;
    }

    public List<SlangEntry> findByWord(String keyword) {
        List<SlangEntry> results = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        Set<Long> ids = wordIndex.get(normalize(keyword));
        if (ids == null) {
            return results;
        }

        for (long id : ids) {
            results.add(entriesById.get(id));
        }

        return results;
    }

    public List<SlangEntry> searchByDefinition(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<SlangEntry> results = definitionIndex.search(normalize(keyword), entriesById);
        history.recordSearch(keyword, SearchHistoryEntry.Mode.DEFINITION, results);
        return results;
    }

    public SlangEntry addWord(String word, List<String> definitions,
            Long overwriteId, String savedFile)
            throws IOException {
        if (word == null || word.trim().isEmpty()
                || definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("Can nhap slang va nghia.");
        }

        String displayWord = word.trim();
        String key = normalize(displayWord);
        SlangEntry oldEntry = null;
        long id;

        if (overwriteId != null) {
            Set<Long> existingIds = wordIndex.get(key);

            if (existingIds == null || !existingIds.contains(overwriteId)) {
                throw new IllegalArgumentException(
                        "ID khong thuoc slang da nhap.");
            }

            id = overwriteId;
            oldEntry = entriesById.get(id);
            displayWord = oldEntry.getWord();
        } else {
            id = 1;

            for (long existingId : entriesById.keySet()) {
                id = Math.max(id, existingId + 1);
            }
        }

        SlangEntry newEntry = new SlangEntry(
                id, displayWord, definitions);

        Set<Long> ids = wordIndex.computeIfAbsent(
                key, ignored -> new LinkedHashSet<>());

        entriesById.put(id, newEntry);
        ids.add(id);
        replaceIndexedDefinitions(oldEntry, newEntry);

        try {
            save(savedFile);
        } catch (IOException e) {
            replaceIndexedDefinitions(newEntry, oldEntry);
            if (oldEntry == null) {
                entriesById.remove(id);
                ids.remove(id);

                if (ids.isEmpty()) {
                    wordIndex.remove(key);
                }
            } else {
                entriesById.put(id, oldEntry);
            }

            throw e;
        }

        return newEntry;
    }

    public SlangEntry editWord(long id, String newWord,
            List<String> newDefinitions, String savedFile) throws IOException {
        SlangEntry oldEntry = entriesById.get(id);

        if (oldEntry == null) {
            throw new IllegalArgumentException("Khong tim thay ID: " + id);
        }

        if (newWord == null || newWord.trim().isEmpty()
                || newDefinitions == null || newDefinitions.isEmpty()) {
            throw new IllegalArgumentException("Can nhap slang va nghia.");
        }

        String oldKey = normalize(oldEntry.getWord());
        String newKey = normalize(newWord);
        boolean keyChanged = !oldKey.equals(newKey);

        SlangEntry updatedEntry = new SlangEntry(
                id, newWord.trim(), newDefinitions);

        Set<Long> oldIds = null;
        entriesById.put(id, updatedEntry);

        if (keyChanged) {
            oldIds = detachWordId(oldKey, id);
            wordIndex.computeIfAbsent(
                    newKey, ignored -> new LinkedHashSet<>()).add(id);
        }

        replaceIndexedDefinitions(oldEntry, updatedEntry);

        try {
            save(savedFile);
        } catch (IOException e) {
            replaceIndexedDefinitions(updatedEntry, oldEntry);
            entriesById.put(id, oldEntry);

            if (keyChanged) {
                wordIndex.put(oldKey, oldIds);

                Set<Long> newIds = wordIndex.get(newKey);
                newIds.remove(id);

                if (newIds.isEmpty()) {
                    wordIndex.remove(newKey);
                }
            }

            throw e;
        }

        return updatedEntry;
    }

    private Set<Long> detachWordId(String key, long id) {
        Set<Long> previousIds = wordIndex.get(key);
        Set<Long> remainingIds = new LinkedHashSet<>(previousIds);
        remainingIds.remove(id);

        if (remainingIds.isEmpty()) {
            wordIndex.remove(key);
        } else {
            wordIndex.put(key, remainingIds);
        }

        return previousIds;
    }

    public SlangEntry deleteWord(long id, String savedFile) throws IOException {
        SlangEntry entry = entriesById.get(id);

        if (entry == null) {
            throw new IllegalArgumentException("Khong tim thay ID: " + id);
        }

        String key = normalize(entry.getWord());
        Set<Long> oldIds = wordIndex.get(key);

        Set<Long> remainingIds = new LinkedHashSet<>(oldIds);
        remainingIds.remove(id);

        entriesById.remove(id);

        if (remainingIds.isEmpty()) {
            wordIndex.remove(key);
        } else {
            wordIndex.put(key, remainingIds);
        }

        definitionIndex.remove(entry);

        try {
            save(savedFile);
        } catch (IOException e) {
            definitionIndex.add(entry);
            entriesById.put(id, entry);
            wordIndex.put(key, oldIds);
            throw e;
        }

        return entry;
    }

    private void replaceIndexedDefinitions(SlangEntry before, SlangEntry after) {
        if (before != null) {
            definitionIndex.remove(before);
        }
        if (after != null) {
            definitionIndex.add(after);
        }
    }

    public int resetToOriginal(String sourceFile, String savedFile)
            throws IOException {
        SlangFileReader reader = new SlangFileReader();
        LoadResult result = reader.readFile(sourceFile);

        if (result.getEntris().isEmpty()) {
            throw new IOException("File goc khong co du lieu hop le.");
        }

        DictionaryService original = new DictionaryService(result.getEntris());

        // Luu thanh cong truoc khi thay du lieu dang dung.
        original.save(savedFile);

        entriesById.clear();
        entriesById.putAll(original.entriesById);

        wordIndex.clear();
        wordIndex.putAll(original.wordIndex);
        definitionIndex = original.definitionIndex;

        return entriesById.size();
    }

    public List<SlangEntry> getAllEntries() {
        List<SlangEntry> entries = new ArrayList<>(entriesById.values());

        Collections.sort(entries, new java.util.Comparator<SlangEntry>() {
            @Override
            public int compare(SlangEntry first, SlangEntry second) {
                return Long.compare(first.getId(), second.getId());
            }
        });

        return entries;
    }

    public QuizQuestion createWordQuiz() {
        return quiz.createWordQuiz();
    }

    public List<SlangEntry> randomSlang() {
        return quiz.randomSlang();
    }

    public QuizQuestion createDefinitionQuiz() {
        return quiz.createDefinitionQuiz();
    }

    public void save(String filename) throws IOException {
        DictionaryFileStore.save(filename, entriesById, wordIndex, definitionIndex);
    }

    public static DictionaryService load(String filename) throws IOException {
        DictionaryService service = new DictionaryService();
        DictionaryFileStore.load(filename, service.entriesById,
                service.wordIndex, service.definitionIndex);
        return service;
    }

    public String Normalize(String text) {
        return normalize(text);
    }

    static String normalize(String text) {
        return text.trim().toLowerCase(Locale.ROOT);
    }
}
