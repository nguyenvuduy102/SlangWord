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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DictionaryFileStore {
    private static final int FILE_FORMAT_V2 = 0xD51A0002;

    private DictionaryFileStore() {
    }

    static void save(String filename, Map<Long, SlangEntry> entriesById,
            Map<String, Set<Long>> wordIndex) throws IOException {
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

    static void load(String filename, Map<Long, SlangEntry> entriesById,
            Map<String, Set<Long>> wordIndex) throws IOException {
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
                if (entriesById.putIfAbsent(id, entry) != null) {
                    throw new IOException("ID bi trung: " + id);
                }
            }

            int keyCount = readCount(in);
            Set<Long> indexedIds = new LinkedHashSet<>();

            for (int i = 0; i < keyCount; i++) {
                String key = in.readUTF();
                int idCount = readCount(in);

                if (idCount == 0 || wordIndex.containsKey(key)) {
                    throw new IOException("Khoa chi muc khong hop le: " + key);
                }

                Set<Long> ids = new LinkedHashSet<>();
                for (int j = 0; j < idCount; j++) {
                    long id = in.readLong();
                    if (!entriesById.containsKey(id) || !indexedIds.add(id)) {
                        throw new IOException("ID chi muc khong hop le: " + id);
                    }
                    ids.add(id);
                }

                wordIndex.put(key, ids);
            }

            if (indexedIds.size() != entryCount) {
                throw new IOException("Chi muc thieu ban ghi.");
            }
        } finally {
            in.close();
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
