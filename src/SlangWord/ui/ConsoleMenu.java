package SlangWord.ui;

import SlangWord.QuizQuestion;
import SlangWord.SearchHistoryEntry;
import SlangWord.SlangEntry;
import SlangWord.service.DictionaryService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ConsoleMenu {
    private final DictionaryService service;
    private final String savedFile;
    private final String sourceFile;

    public ConsoleMenu(DictionaryService service, String savedFile, String sourceFile) {
        this.service = service;
        this.savedFile = savedFile;
        this.sourceFile = sourceFile;
    }

    public void run() throws IOException {
        BufferedReader keyboard = new BufferedReader(
                new InputStreamReader(System.in));

        while (true) {
            printMenu();
            String choice = keyboard.readLine();

            if (choice == null) {
                return;
            }

            choice = choice.trim();

            switch (choice) {
                case "1":
                case "2":
                    System.out.print("Nhap tu khoa: ");
                    String keyword = keyboard.readLine();

                    if (keyword == null) {
                        return;
                    }

                    if (keyword.trim().isEmpty()) {
                        System.out.println("Vui long nhap tu khoa.");
                        break;
                    }

                    showSearchResults(keyword, choice.equals("1"));
                    break;

                case "3":
                    showHistory();
                    break;

                case "4":
                    addSlang(keyboard);
                    break;

                case "5":
                    editSlang(keyboard);
                    break;

                case "6":
                    deleteSlang(keyboard);
                    break;

                case "7":
                    resetDictionary(keyboard);
                    break;

                case "8":
                    showRandomSlang();
                    break;

                case "9":
                    playQuiz(keyboard, service.createWordQuiz(), "Slang");
                    break;

                case "10":
                    playQuiz(keyboard, service.createDefinitionQuiz(), "Definition");
                    break;

                case "0":
                    return;

                default:
                    System.out.println("Lua chon khong hop le.");
            }
        }
    }

    private void addSlang(BufferedReader keyboard) throws IOException {
        System.out.print("Nhap slang: ");
        String word = keyboard.readLine();

        if (word == null || word.trim().isEmpty()) {
            System.out.println("Can nhap slang.");
            return;
        }

        List<SlangEntry> existing = service.findByWord(word);
        Long overwriteId = null;

        if (!existing.isEmpty()) {
            int choice = readDuplicateChoice(keyboard, existing);
            if (choice == 0) {
                return;
            }
            if (choice == 1) {
                overwriteId = readOverwriteId(keyboard, existing);
                if (overwriteId == null) {
                    return;
                }
            }
        }

        System.out.print("Nhap nghia, cach nhau bang |: ");
        String text = keyboard.readLine();

        if (text == null) {
            return;
        }

        List<String> definitions = new ArrayList<>();

        for (String part : text.split("\\|")) {
            String definition = part.trim();

            if (!definition.isEmpty()) {
                definitions.add(definition);
            }
        }

        try {
            SlangEntry saved = service.addWord(
                    word, definitions, overwriteId, savedFile);
            System.out.println("Da luu: " + saved);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Luu that bai: " + e.getMessage());
        }
    }

    private int readDuplicateChoice(BufferedReader keyboard, List<SlangEntry> existing)
            throws IOException {
        System.out.println("Slang da ton tai:");

        for (SlangEntry entry : existing) {
            System.out.println(entry);
        }

        System.out.println("1. Ghi de mot ban ghi");
        System.out.println("2. Duplicate voi cung ten va ID moi");
        System.out.println("0. Huy");
        System.out.print("Chon: ");
        String choice = keyboard.readLine();

        if (choice == null || choice.trim().equals("0")) {
            return 0;
        }

        switch (choice.trim()) {
            case "1":
                return 1;
            case "2":
                return 2;
            default:
                System.out.println("Lua chon khong hop le.");
                return 0;
        }
    }

    private Long readOverwriteId(BufferedReader keyboard, List<SlangEntry> existing) throws IOException {
        if (existing.size() == 1) {
            return existing.get(0).getId();
        }

        System.out.print("Nhap ID can ghi de: ");
        String text = keyboard.readLine();
        if (text == null) {
            return null;
        }

        long id;
        try {
            id = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            System.out.println("ID khong hop le.");
            return null;
        }

        for (SlangEntry entry : existing) {
            if (entry.getId() == id) {
                return id;
            }
        }

        System.out.println("ID khong thuoc slang can ghi de.");
        return null;
    }

    private void editSlang(BufferedReader keyboard) throws IOException {
        System.out.print("Nhap slang can sua: ");
        String keyword = keyboard.readLine();

        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        List<SlangEntry> matches = service.findByWord(keyword);

        if (matches.isEmpty()) {
            System.out.println("Khong tim thay slang.");
            return;
        }

        for (SlangEntry entry : matches) {
            System.out.println(entry);
        }

        SlangEntry selected = matches.get(0);

        if (matches.size() > 1) {
            System.out.print("Nhap ID cua ban ghi can sua: ");
            String idText = keyboard.readLine();

            if (idText == null) {
                return;
            }

            long id;

            try {
                id = Long.parseLong(idText.trim());
            } catch (NumberFormatException e) {
                System.out.println("ID khong hop le.");
                return;
            }

            selected = null;

            for (SlangEntry entry : matches) {
                if (entry.getId() == id) {
                    selected = entry;
                    break;
                }
            }

            if (selected == null) {
                System.out.println("ID khong thuoc slang da nhap.");
                return;
            }
        }

        System.out.print("Nhap ten moi (Enter de giu ten cu): ");
        String newWord = keyboard.readLine();

        if (newWord == null) {
            return;
        }

        newWord = newWord.trim();

        if (newWord.isEmpty()) {
            newWord = selected.getWord();
        }

        System.out.print("Nhap nghia moi, cach nhau bang | (Enter de giu nghia cu): ");
        String text = keyboard.readLine();

        if (text == null) {
            return;
        }

        List<String> definitions = new ArrayList<>();

        if (text.trim().isEmpty()) {
            definitions.addAll(selected.getDefinitions());
        } else {
            for (String part : text.split("\\|")) {
                String definition = part.trim();

                if (!definition.isEmpty()) {
                    definitions.add(definition);
                }
            }
        }

        if (definitions.isEmpty()) {
            System.out.println("Can nhap it nhat mot nghia.");
            return;
        }

        String oldKey = service.Normalize(selected.getWord());
        String newKey = service.Normalize(newWord);

        if (!oldKey.equals(newKey) && !service.findByWord(newWord).isEmpty()) {
            System.out.print("Ten moi da ton tai. Xac nhan doi ten va giu cac ban ghi cung ten? (y/n): ");
            String confirmation = keyboard.readLine();
            if (confirmation == null || !confirmation.trim().equalsIgnoreCase("y")) {
                return;
            }
        }

        try {
            SlangEntry updated = service.editWord(
                    selected.getId(), newWord, definitions, savedFile);

            System.out.println("Da sua: " + updated);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Luu that bai: " + e.getMessage());
        }
    }

    private void deleteSlang(BufferedReader keyboard) throws IOException {
        System.out.print("Nhap slang can xoa: ");
        String word = keyboard.readLine();

        if (word == null || word.trim().isEmpty()) {
            return;
        }

        List<SlangEntry> matches = service.findByWord(word);

        if (matches.isEmpty()) {
            System.out.println("Khong tim thay slang.");
            return;
        }

        SlangEntry selected = matches.get(0);

        if (matches.size() > 1) {
            for (SlangEntry entry : matches) {
                System.out.println(entry);
            }

            System.out.print("Nhap ID cua ban ghi can xoa: ");
            String idText = keyboard.readLine();

            if (idText == null) {
                return;
            }

            long id;

            try {
                id = Long.parseLong(idText.trim());
            } catch (NumberFormatException e) {
                System.out.println("ID khong hop le.");
                return;
            }

            selected = null;

            for (SlangEntry entry : matches) {
                if (entry.getId() == id) {
                    selected = entry;
                    break;
                }
            }

            if (selected == null) {
                System.out.println("ID khong thuoc slang da nhap.");
                return;
            }
        }

        System.out.println("Ban ghi se xoa: " + selected);
        System.out.print("Xac nhan xoa? (y/n): ");
        String confirmation = keyboard.readLine();

        if (confirmation == null || !confirmation.trim().equalsIgnoreCase("y")) {
            System.out.println("Da huy xoa.");
            return;
        }

        try {
            SlangEntry deleted = service.deleteWord(selected.getId(), savedFile);
            System.out.println("Da xoa: " + deleted);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Luu that bai: " + e.getMessage());
        }
    }

    private void resetDictionary(BufferedReader keyboard) throws IOException {
        System.out.println("Reset se bo cac thay doi them, sua, xoa.");
        System.out.print("Khoi phuc tu dien goc? (y/n): ");
        String confirmation = keyboard.readLine();

        if (confirmation == null || !confirmation.trim().equalsIgnoreCase("y")) {
            System.out.println("Da huy reset.");
            return;
        }

        try {
            int restoredCount = service.resetToOriginal(sourceFile, savedFile);
            System.out.println(
                    "Da khoi phuc " + restoredCount + " ban ghi tu file goc.");
        } catch (IOException e) {
            System.out.println("Reset that bai: " + e.getMessage());
        }
    }

    private void showRandomSlang() {
        List<SlangEntry> entries = service.randomSlang();

        if (entries.isEmpty()) {
            System.out.println("Tu dien rong.");
            return;
        }

        System.out.println("On this day slang word: " + entries.get(0).getWord());

        for (SlangEntry entry : entries) {
            System.out.println(entry);
        }
    }

    private void playQuiz(BufferedReader keyboard, QuizQuestion question,
            String promptLabel) throws IOException {

        if (question == null) {
            System.out.println("Khong du dap an phan biet de tao cau hoi.");
            return;
        }

        System.out.println(promptLabel + ": " + question.getPrompt());
        List<String> choices = question.getChoices();

        for (int i = 0; i < choices.size(); i++) {
            System.out.println((i + 1) + ". " + choices.get(i));
        }

        while (true) {
            System.out.print("Chon dap an (1-4, 0 de huy): ");
            String input = keyboard.readLine();

            if (input == null || input.trim().equals("0")) {
                return;
            }

            int choice;

            try {
                choice = Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("Vui long nhap so tu 1 den 4.");
                continue;
            }

            if (choice < 1 || choice > choices.size()) {
                System.out.println("Vui long chon tu 1 den 4.");
                continue;
            }

            if (question.isCorrect(choice - 1)) {
                System.out.println("Chinh xac!");
            } else {
                System.out.println("Dap an dung: " + question.getCorrectAnswer());
            }

            return;
        }
    }

    private void printMenu() {
        System.out.println("\n===== SLANG DICTIONARY =====");
        System.out.println("1.  Tim theo slang word");
        System.out.println("2.  Tim theo definition");
        System.out.println("3.  Xem lich su tra slang");
        System.out.println("4.  Them slang");
        System.out.println("5.  Sua slang");
        System.out.println("6.  Xoa slang");
        System.out.println("7.  Khoi phuc tu dien goc");
        System.out.println("8.  Hien thi slang ngau nhien");
        System.out.println("9.  Do vui: Cho slang, chon nghia");
        System.out.println("10. Do vui: Cho nghia, chon slang");
        System.out.println("0.  Thoat");
        System.out.print("Chon: ");
    }

    private void showSearchResults(String keyword, boolean searchByWord) {
        List<SlangEntry> results;
        long start = System.nanoTime();

        if (searchByWord) {
            results = service.searchByWord(keyword);
        } else {
            results = service.searchByDefinition(keyword);
        }

        double milliseconds = (System.nanoTime() - start) / 1_000_000.0;

        if (results.isEmpty()) {
            System.out.println("Khong tim thay.");
        } else {
            System.out.println("So ket qua: " + results.size());

            for (SlangEntry entry : results) {
                System.out.println(entry);
            }
        }

        System.out.printf("Thoi gian tim: %.3f ms%n", milliseconds);
    }

    private void showHistory() {
        List<SearchHistoryEntry> history = service.getSearchHistory();

        if (history.isEmpty()) {
            System.out.println("Chua co lich su tra cuu.");
            return;
        }

        System.out.println("\n===== LICH SU TRA CUU =====");
        for (int i = history.size() - 1; i >= 0; i--) {
            SearchHistoryEntry entry = history.get(i);
            System.out.printf("%d. %s | %s | %s | %s | %s%n",
                    history.size() - i, entry.getFormattedTime(), entry.getKeyword(),
                    entry.getMode().getLabel(), entry.getResultLabel(), entry.getDetails());
        }
    }
}