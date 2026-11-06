package SlangWord;

import java.util.List;

public class SlangEntry {
    private final long id;
    private final String word;
    private final List<String> definitions;

    public SlangEntry(long id, String word, List<String> definitions ) {
         this.id = id;
         this.word = word;
         this. definitions = definitions;
    }

    @Override 
    public String toString() {
        return id + "| " + word + " | " + String.join(";", definitions);
    }
}
