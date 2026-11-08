package SlangWord.service;

import SlangWord.QuizQuestion;
import SlangWord.SearchHistoryEntry;
import SlangWord.SlangEntry;
import SlangWord.io.LoadResult;
import SlangWord.io.SearchHistoryStore;
import SlangWord.io.SlangFileReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class DictionaryService {
    private static final int FILE_FORMAT_V2 = 0xD51A0002;

    private final Map<Long, SlangEntry> entriesById = new HashMap<>();
    private final Map<String, Set<Long>> wordIndex = new HashMap<>();
    private final List<SearchHistoryEntry> searchHistory = new ArrayList<>();
    private SearchHistoryStore historyStore;
    private String searchHistoryError;

    private DictionaryService() {
    }

    public DictionaryService(List<SlangEntry> entries) {
        for (SlangEntry entry : entries) {
            long id = entry.getId();
            if (entriesById.putIfAbsent(id, entry) != null) {
                throw new IllegalArgumentException("Duplicate entry ID: " + id);
            }

            String key = Normalize(entry.getWord());
            wordIndex.computeIfAbsent(key, word -> new LinkedHashSet<>()).add(id);
        }
    }

    public void openSearchHistory(String filename) {
        historyStore = new SearchHistoryStore(filename);

        try {
            List<SearchHistoryEntry> savedHistory = historyStore.load();
            searchHistory.clear();
            searchHistory.addAll(savedHistory);
            searchHistoryError = null;
        } catch (IOException e) {
            searchHistoryError = "Không đọc được lịch sử: " + e.getMessage();
        }
    }

    public List<SearchHistoryEntry> getSearchHistory() {
        return new ArrayList<>(searchHistory);
    }

    public int getSearchHistoryCount() {
        return searchHistory.size();
    }

    public String getSearchHistoryError() {
        return searchHistoryError;
    }

    public List<SlangEntry> searchByWord(String keyword) {
        List<SlangEntry> results = findByWord(keyword);

        if (keyword != null && !keyword.trim().isEmpty()) {
            recordSearch(keyword, SearchHistoryEntry.Mode.WORD, results);
        }

        return results;
    }

    public List<SlangEntry> findByWord(String keyword) {
        List<SlangEntry> results = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        Set<Long> ids = wordIndex.get(Normalize(keyword));
        if (ids == null) {
            return results;
        }

        for (long id : ids) {
            results.add(entriesById.get(id));
        }

        return results;
    }

    public List<SlangEntry> searchByDefinition(String keyword) {
        List<SlangEntry> results = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        String query = Normalize(keyword);

        for (SlangEntry entry : entriesById.values()) {
            for (String definition : entry.getDefinitions()) {
                if (definition.toLowerCase(Locale.ROOT).contains(query)) {
                    results.add(entry);
                    break;
                }
            }
        }

        recordSearch(keyword, SearchHistoryEntry.Mode.DEFINITION, results);
        return results;
    }

    private void recordSearch(String keyword, SearchHistoryEntry.Mode mode,
            List<SlangEntry> results) {
        SearchHistoryEntry entry = new SearchHistoryEntry(
                System.currentTimeMillis(), keyword.trim(), mode, results.size(),
                historyPreview(mode, results));

        searchHistory.add(entry);

        if (historyStore != null) {
            try {
                historyStore.append(entry);
                searchHistoryError = null;
            } catch (IOException e) {
                searchHistoryError = "Không lưu được lượt tra cứu: " + e.getMessage();
            }
        }
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

    public SlangEntry addWord(String word, List<String> definitions,
            Long overwriteId, String savedFile)
            throws IOException {
        if (word == null || word.trim().isEmpty()
                || definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("Can nhap slang va nghia.");
        }

        String displayWord = word.trim();
        String key = Normalize(displayWord);
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
                id, displayWord, new ArrayList<>(definitions));

        Set<Long> ids = wordIndex.computeIfAbsent(
                key, ignored -> new LinkedHashSet<>());

        entriesById.put(id, newEntry);
        ids.add(id);

        try {
            save(savedFile);
        } catch (IOException e) {
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
            List<String> newDefinitions, String savedFile)
            throws IOException {
        SlangEntry oldEntry = entriesById.get(id);

        if (oldEntry == null) {
            throw new IllegalArgumentException("Khong tim thay ID: " + id);
        }

        if (newWord == null || newWord.trim().isEmpty()
                || newDefinitions == null || newDefinitions.isEmpty()) {
            throw new IllegalArgumentException("Can nhap slang va nghia.");
        }

        String oldKey = Normalize(oldEntry.getWord());
        String newKey = Normalize(newWord);
        boolean keyChanged = !oldKey.equals(newKey);

        SlangEntry updatedEntry = new SlangEntry(
                id, newWord.trim(), new ArrayList<>(newDefinitions));

        Set<Long> oldIds = wordIndex.get(oldKey);

        entriesById.put(id, updatedEntry);

        if (keyChanged) {
            Set<Long> remainingIds = new LinkedHashSet<>(oldIds);
            remainingIds.remove(id);

            if (remainingIds.isEmpty()) {
                wordIndex.remove(oldKey);
            } else {
                wordIndex.put(oldKey, remainingIds);
            }

            wordIndex.computeIfAbsent(
                    newKey, ignored -> new LinkedHashSet<>()).add(id);
        }

        try {
            save(savedFile);
        } catch (IOException e) {
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

    public SlangEntry deleteWord(long id, String savedFile) throws IOException {
        SlangEntry entry = entriesById.get(id);

        if (entry == null) {
            throw new IllegalArgumentException("Khong tim thay ID: " + id);
        }

        String key = Normalize(entry.getWord());
        Set<Long> oldIds = wordIndex.get(key);

        Set<Long> remainingIds = new LinkedHashSet<>(oldIds);
        remainingIds.remove(id);

        entriesById.remove(id);

        if (remainingIds.isEmpty()) {
            wordIndex.remove(key);
        } else {
            wordIndex.put(key, remainingIds);
        }

        try {
            save(savedFile);
        } catch (IOException e) {
            entriesById.put(id, entry);
            wordIndex.put(key, oldIds);
            throw e;
        }

        return entry;
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

    private final Random random = new Random();

    public QuizQuestion createWordQuiz() {
        if (wordIndex.isEmpty()) {
            return null;
        }

        // Chon theo khoa slang de tu co nhieu ID van chi duoc tinh mot lan.
        List<String> keys = new ArrayList<>(wordIndex.keySet());
        String key = keys.get(random.nextInt(keys.size()));
        Set<Long> questionIds = wordIndex.get(key);
        String questionWord = entriesById.get(questionIds.iterator().next()).getWord();

        // Lay cac nghia dung tu tat ca ID cua slang.
        Map<String, String> correctByText = new HashMap<>();

        for (long id : questionIds) {
            SlangEntry entry = entriesById.get(id);

            for (String definition : entry.getDefinitions()) {
                String textKey = Normalize(definition);

                if (!textKey.isEmpty()) {
                    correctByText.putIfAbsent(textKey, definition);
                }
            }
        }

        if (correctByText.isEmpty()) {
            return null;
        }

        // Lay nghia tu cac slang khac va loai cac nghia da duoc xem la dung.
        Map<String, String> wrongByText = new HashMap<>();

        for (SlangEntry entry : entriesById.values()) {
            if (questionIds.contains(entry.getId())) {
                continue;
            }

            for (String definition : entry.getDefinitions()) {
                String textKey = Normalize(definition);

                if (!textKey.isEmpty() && !correctByText.containsKey(textKey)) {
                    wrongByText.putIfAbsent(textKey, definition);
                }
            }
        }

        if (wrongByText.size() < 3) {
            return null;
        }

        List<String> correctOptions = new ArrayList<>(correctByText.values());
        String correctAnswer = correctOptions.get(
                random.nextInt(correctOptions.size()));

        List<String> wrongOptions = new ArrayList<>(wrongByText.values());
        Collections.shuffle(wrongOptions, random);

        List<String> choices = new ArrayList<>(wrongOptions.subList(0, 3));
        choices.add(correctAnswer);
        Collections.shuffle(choices, random);

        int correctIndex = choices.indexOf(correctAnswer);
        return new QuizQuestion(questionWord, choices, correctIndex);
    }

    public List<SlangEntry> randomSlang() {
        if (wordIndex.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keys = new ArrayList<>(wordIndex.keySet());
        String key = keys.get(random.nextInt(keys.size()));

        return findByWord(key);
    }

    public QuizQuestion createDefinitionQuiz() {
        if (wordIndex.size() < 4) {
            return null;
        }

        Map<String, Set<String>> wordsByDefinition = new HashMap<>();
        Map<String, String> displayDefinitions = new HashMap<>();

        for (Map.Entry<String, Set<Long>> item : wordIndex.entrySet()) {
            String wordKey = item.getKey();

            for (long id : item.getValue()) {
                for (String definition : entriesById.get(id).getDefinitions()) {
                    String definitionKey = Normalize(definition);

                    if (!definitionKey.isEmpty()) {
                        wordsByDefinition.computeIfAbsent(
                                definitionKey, ignored -> new LinkedHashSet<>()).add(wordKey);
                        displayDefinitions.putIfAbsent(definitionKey, definition);
                    }
                }
            }
        }

        List<String> eligibleDefinitions = new ArrayList<>();

        for (Map.Entry<String, Set<String>> item : wordsByDefinition.entrySet()) {
            if (wordIndex.size() - item.getValue().size() >= 3) {
                eligibleDefinitions.add(item.getKey());
            }
        }

        if (eligibleDefinitions.isEmpty()) {
            return null;
        }

        String definitionKey = eligibleDefinitions.get(
                random.nextInt(eligibleDefinitions.size()));

        Set<String> matchingWords = wordsByDefinition.get(definitionKey);
        List<String> correctKeys = new ArrayList<>(matchingWords);
        String correctWord = displayWord(correctKeys.get(random.nextInt(correctKeys.size())));

        List<String> wrongKeys = new ArrayList<>(wordIndex.keySet());
        wrongKeys.removeAll(matchingWords);
        Collections.shuffle(wrongKeys, random);

        List<String> choices = new ArrayList<>();
        choices.add(correctWord);

        for (int i = 0; i < 3; i++) {
            choices.add(displayWord(wrongKeys.get(i)));
        }

        Collections.shuffle(choices, random);

        return new QuizQuestion(displayDefinitions.get(definitionKey),
                choices, choices.indexOf(correctWord));
    }

    private String displayWord(String key) {
        long id = wordIndex.get(key).iterator().next();
        return entriesById.get(id).getWord();
    }

    public void save(String filename) throws IOException {
        Path target = Paths.get(filename).toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), "slang-", ".tmp");

        try {
            DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(temporary.toFile())));

            try {
                out.writeInt(FILE_FORMAT_V2);

                // Ghi cac ban ghi theo ID.
                out.writeInt(entriesById.size());
                for (SlangEntry entry : entriesById.values()) {
                    out.writeLong(entry.getId());
                    out.writeUTF(entry.getWord());
                    out.writeInt(entry.getDefinitions().size());

                    for (String definition : entry.getDefinitions()) {
                        out.writeUTF(definition);
                    }
                }

                // Ghi khoa da chuan hoa va danh sach ID tuong ung.
                out.writeInt(wordIndex.size());
                for (Map.Entry<String, Set<Long>> item : wordIndex.entrySet()) {
                    out.writeUTF(item.getKey());
                    out.writeInt(item.getValue().size());

                    for (long id : item.getValue()) {
                        out.writeLong(id);
                    }
                }
            } finally {
                out.close();
            }

            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static DictionaryService load(String filename) throws IOException {
        DictionaryService service = new DictionaryService();
        DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filename)));

        try {
            if (in.readInt() != FILE_FORMAT_V2) {
                throw new IOException("Dinh dang chi muc khong hop le.");
            }

            int entryCount = readCount(in);
            for (int i = 0; i < entryCount; i++) {
                long id = in.readLong();
                String word = in.readUTF();
                int definitionCount = readCount(in);
                List<String> definitions = new ArrayList<>();

                for (int j = 0; j < definitionCount; j++) {
                    definitions.add(in.readUTF());
                }

                SlangEntry entry = new SlangEntry(id, word, definitions);
                if (service.entriesById.putIfAbsent(id, entry) != null) {
                    throw new IOException("ID bi trung: " + id);
                }
            }

            int keyCount = readCount(in);
            Set<Long> indexedIds = new LinkedHashSet<>();

            for (int i = 0; i < keyCount; i++) {
                String key = in.readUTF();
                int idCount = readCount(in);

                if (idCount == 0 || service.wordIndex.containsKey(key)) {
                    throw new IOException("Khoa chi muc khong hop le: " + key);
                }

                Set<Long> ids = new LinkedHashSet<>();
                for (int j = 0; j < idCount; j++) {
                    long id = in.readLong();
                    if (!service.entriesById.containsKey(id) || !indexedIds.add(id)) {
                        throw new IOException("ID chi muc khong hop le: " + id);
                    }
                    ids.add(id);
                }

                service.wordIndex.put(key, ids);
            }

            if (indexedIds.size() != entryCount) {
                throw new IOException("Chi muc thieu ban ghi.");
            }
        } finally {
            in.close();
        }

        return service;
    }

    private static int readCount(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0) {
            throw new IOException("So luong ban ghi khong hop le.");
        }
        return count;
    }

    public String Normalize(String text) {
        return text.trim().toLowerCase(Locale.ROOT);
    }
}
