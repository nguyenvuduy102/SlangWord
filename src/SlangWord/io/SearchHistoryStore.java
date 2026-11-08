package SlangWord.io;

import SlangWord.SearchHistoryEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SearchHistoryStore {
    private final File file;

    public SearchHistoryStore(String filename) {
        file = new File(filename);
    }

    public List<SearchHistoryEntry> load() throws IOException {
        List<SearchHistoryEntry> history = new ArrayList<>();

        if (Files.notExists(file.toPath())) {
            return history;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                SearchHistoryEntry entry = decode(line);

                if (entry != null) {
                    history.add(entry);
                }
            }
        }

        return history;
    }

    public void append(SearchHistoryEntry entry) throws IOException {
        // Base64 preserves tabs, newlines and Unicode within each TSV field.
        String line = entry.getTimestamp() + "\t" + entry.getMode().name() + "\t"
                + entry.getResultCount() + "\t" + encode(entry.getKeyword())
                + "\t" + encode(entry.getDetails()) + "\n";
        byte[] data = line.getBytes(StandardCharsets.UTF_8);

        try (RandomAccessFile out = new RandomAccessFile(file, "rw");
                FileLock lock = out.getChannel().lock()) {
            long previousLength = out.length();
            boolean needsSeparator = false;

            if (previousLength > 0) {
                out.seek(previousLength - 1);
                needsSeparator = out.read() != '\n';
            }

            out.seek(previousLength);

            try {
                if (needsSeparator) {
                    out.write('\n');
                }
                out.write(data);
            } catch (IOException e) {
                try {
                    out.setLength(previousLength);
                } catch (IOException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }

    private String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private SearchHistoryEntry decode(String line) {
        String[] fields = line.split("\t", -1);

        if (fields.length != 5) {
            return null;
        }

        try {
            long timestamp = Long.parseLong(fields[0]);
            SearchHistoryEntry.Mode mode = SearchHistoryEntry.Mode.valueOf(fields[1]);
            int resultCount = Integer.parseInt(fields[2]);

            if (resultCount < 0) {
                return null;
            }

            Base64.Decoder decoder = Base64.getDecoder();
            String keyword = new String(decoder.decode(fields[3]), StandardCharsets.UTF_8);
            String details = new String(decoder.decode(fields[4]), StandardCharsets.UTF_8);

            return new SearchHistoryEntry(timestamp, keyword, mode, resultCount, details);
        } catch (IllegalArgumentException e) {
            // Skip an incomplete or malformed row and keep the other records.
            return null;
        }
    }
}
