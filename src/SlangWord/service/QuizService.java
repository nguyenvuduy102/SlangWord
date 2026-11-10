package SlangWord.service;

import SlangWord.QuizQuestion;
import SlangWord.SlangEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static SlangWord.service.DictionaryService.normalize;

final class QuizService {
    private final Map<Long, SlangEntry> entriesById;
    private final Map<String, Set<Long>> wordIndex;
    private final Random random = new Random();

    QuizService(Map<Long, SlangEntry> entriesById, Map<String, Set<Long>> wordIndex) {
        this.entriesById = entriesById;
        this.wordIndex = wordIndex;
    }

    QuizQuestion createWordQuiz() {
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
                String textKey = normalize(definition);

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
                String textKey = normalize(definition);

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

    List<SlangEntry> randomSlang() {
        if (wordIndex.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keys = new ArrayList<>(wordIndex.keySet());
        String key = keys.get(random.nextInt(keys.size()));

        List<SlangEntry> entries = new ArrayList<>();
        for (long id : wordIndex.get(key)) {
            entries.add(entriesById.get(id));
        }
        return entries;
    }

    QuizQuestion createDefinitionQuiz() {
        if (wordIndex.size() < 4) {
            return null;
        }

        Map<String, Set<String>> wordsByDefinition = new HashMap<>();
        Map<String, String> displayDefinitions = new HashMap<>();

        for (Map.Entry<String, Set<Long>> item : wordIndex.entrySet()) {
            String wordKey = item.getKey();

            for (long id : item.getValue()) {
                for (String definition : entriesById.get(id).getDefinitions()) {
                    String definitionKey = normalize(definition);

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
}
