package SlangWord;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class SearchHistoryEntry {
    public enum Mode {
        WORD("Slang Word"),
        DEFINITION("By Definition");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final long timestamp;
    private final String keyword;
    private final Mode mode;
    private final int resultCount;
    private final String details;

    public SearchHistoryEntry(long timestamp, String keyword, Mode mode,
            int resultCount, String details) {
        this.timestamp = timestamp;
        this.keyword = keyword;
        this.mode = mode;
        this.resultCount = resultCount;
        this.details = details;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        return TIME_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

    public String getKeyword() {
        return keyword;
    }

    public Mode getMode() {
        return mode;
    }

    public int getResultCount() {
        return resultCount;
    }

    public String getResultLabel() {
        return resultCount == 0 ? "Không tìm thấy" : "Tìm thấy (" + resultCount + ")";
    }

    public String getDetails() {
        return details;
    }
}
