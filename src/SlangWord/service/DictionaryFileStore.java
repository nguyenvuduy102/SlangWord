package SlangWord.service;

import SlangWord.SlangEntry;

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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class DictionaryFileStore {
    private static final int FILE_FORMAT_V3 = 0xD51A0003;

    private DictionaryFileStore() {
    }

    static void save(String filename, Map<Long, SlangEntry> entriesById,
            Map<String, Set<Long>> wordIndex, DefinitionTrigramIndex definitionIndex) throws IOException {
        Path target = Paths.get(filename).toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), "slang-", ".tmp");

        try {
            DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(temporary.toFile())));

            try {
                out.writeInt(FILE_FORMAT_V3);

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

                writeTrigramIndex(out, definitionIndex);
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

    static void load(String filename, Map<Long, SlangEntry> entriesById,
            Map<String, Set<Long>> wordIndex, DefinitionTrigramIndex definitionIndex) throws IOException {
        DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filename)));

        try {
            if (in.readInt() != FILE_FORMAT_V3) {
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
                if (entriesById.putIfAbsent(id, entry) != null) {
                    throw new IOException("ID bi trung: " + id);
                }
            }

            int keyCount = readCount(in);
            Map<Long, Long> indexedIds = new HashMap<>();

            for (int i = 0; i < keyCount; i++) {
                String key = in.readUTF();
                int idCount = readCount(in);

                if (idCount == 0 || wordIndex.containsKey(key)) {
                    throw new IOException("Khoa chi muc khong hop le: " + key);
                }

                Set<Long> ids = new LinkedHashSet<>();
                for (int j = 0; j < idCount; j++) {
                    Long id = in.readLong();
                    if (!entriesById.containsKey(id) || indexedIds.putIfAbsent(id, id) != null) {
                        throw new IOException("ID chi muc khong hop le: " + id);
                    }
                    ids.add(id);
                }

                wordIndex.put(key, ids);
            }

            if (indexedIds.size() != entryCount) {
                throw new IOException("Chi muc thieu ban ghi.");
            }

            readTrigramIndex(in, indexedIds, definitionIndex);
        } finally {
            in.close();
        }
    }

    private static void writeTrigramIndex(DataOutputStream out,
            DefinitionTrigramIndex index) throws IOException {
        out.writeInt(index.getPostings().size());
        for (Map.Entry<String, TreeSet<Long>> item : index.getPostings().entrySet()) {
            out.writeUTF(item.getKey());
            TreeSet<Long> posting = item.getValue();
            out.writeInt(posting.size());
            for (long id : posting) {
                out.writeLong(id);
            }
        }
    }

    private static void readTrigramIndex(DataInputStream in, Map<Long, Long> indexedIds,
            DefinitionTrigramIndex index) throws IOException {
        int gramCount = readCount(in);
        for (int i = 0; i < gramCount; i++) {
            String gram = in.readUTF();
            int idCount = readCount(in);
            if (gram.codePointCount(0, gram.length()) != DefinitionTrigramIndex.GRAM_SIZE
                    || idCount == 0 || idCount > indexedIds.size()
                    || index.getPostings().containsKey(gram)) {
                throw new IOException("Trigram khong hop le: " + gram);
            }

            TreeSet<Long> ids = new TreeSet<>();
            long previousId = 0;
            for (int j = 0; j < idCount; j++) {
                long storedId = in.readLong();
                // Reuse the ID object from the word index across all trigrams.
                Long id = indexedIds.get(storedId);
                if ((j > 0 && storedId <= previousId) || id == null) {
                    throw new IOException("ID Trigram khong hop le: " + storedId);
                }
                ids.add(id);
                previousId = storedId;
            }
            index.getPostings().put(gram, ids);
        }
    }

    private static int readCount(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0) {
            throw new IOException("So luong ban ghi khong hop le.");
        }
        return count;
    }
}
