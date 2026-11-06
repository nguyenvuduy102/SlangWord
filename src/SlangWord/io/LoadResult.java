package SlangWord.io;
import SlangWord.SlangEntry;

import java.util.ArrayList;
import java.util.List;

public class LoadResult {
    private List<SlangEntry> entries = new ArrayList<>();
    private List<DataIssue> issues = new ArrayList<>();

    public void addEntry(SlangEntry se) {
        entries.add(se);
    }

    public void addDataIssue(DataIssue is) {
        issues.add(is);
    }

    public List<DataIssue> getIssue() {
        return this.issues;
    }

    public List<SlangEntry> getEntris() {
        return this.entries;
    }
}
