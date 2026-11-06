package SlangWord;

import SlangWord.io.DataIssue;
import SlangWord.io.LoadResult;
import SlangWord.io.SlangFileReader;

public class Main {
    public static void main(String[] args) {
        String fileName = "../data/slang.txt";

        SlangFileReader reader = new SlangFileReader();
        LoadResult result = reader.readFile(fileName);

        System.out.println("=== KET QUA DOC FILE ===");
        System.out.println("So ban ghi: " + result.getEntris().size());
        System.out.println("So van de: " + result.getIssue().size());

        System.out.println("\n=== 5 BAN GHI DAU ===");

        int count = Math.min(5, result.getEntris().size());

        for (int i = 0; i < count; i++) {
            SlangEntry entry = result.getEntris().get(i);
            System.out.println(entry);
        }

        System.out.println("\n=== CAC DONG CO VAN DE ===");

        for (DataIssue issue : result.getIssue()) {
            System.out.println(issue);
        }
    }
}