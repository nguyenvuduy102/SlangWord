package SlangWord.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import SlangWord.SlangEntry;

public class DictionaryService {
    private final Map<String, SlangEntry> dictionary;
    private final List<String> searchHistory = new ArrayList<>();

    public DictionaryService(List<SlangEntry> entries) {
        dictionary = new HashMap<>();
        for(SlangEntry entry : entries) {
            String key = Normalize(entry.getWord()) ;
            if(!dictionary.containsKey(key)) {
                dictionary.put(key, entry );
            }
        }
    }

    public DictionaryService(Map<String, SlangEntry> map) {
        dictionary = map;
    }

    public List<String> getSearchHistory()
    {
        return new ArrayList<>(searchHistory);
    }

    public SlangEntry searchByWord(String key) {
        key = Normalize(key).trim();
        if(key.trim().isEmpty()) {
            return null;
        }
        SlangEntry entry = dictionary.get(key);
        if(entry != null) {
            searchHistory.add(entry.getWord());
        }

        return entry;
    }

    public List<SlangEntry> searchByDefinition(String keyword)
{
    List<SlangEntry> results = new ArrayList<>();

    if (keyword == null || keyword.trim().isEmpty())
    {
        return results;
    }

    String query = keyword.trim().toLowerCase(Locale.ROOT);

    for (SlangEntry entry : dictionary.values())
    {
        for (String definition : entry.getDefinitions())
        {
            String text = definition.toLowerCase(Locale.ROOT);

            if (text.contains(query))
            {
                results.add(entry);
                break;
            }
        }
    }

    return results;
}  

    public void save(String filename) throws IOException {
         DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));

         try {
            out.writeInt(dictionary.size());

            for(Map.Entry<String, SlangEntry> item : dictionary.entrySet()) {
                
                out.writeUTF(item.getKey());
                SlangEntry entry = item.getValue();

                out.writeLong(entry.getId());
                out.writeUTF(entry.getWord());
                out.writeInt(entry.getDefinitions().size());
                for(String def : entry.getDefinitions()) {
                    out.writeUTF(def);
                }
            }

         } finally {
            out.close();
         }

    }

    public static DictionaryService load(String filename) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
        Map<String, SlangEntry> dic = new HashMap<>();
        try {
            int n = in.readInt();
            for(int i = 0; i < n; i++) {
                String key = in.readUTF();
                long id = in.readLong();
                String word = in.readUTF();
                int size = in.readInt();
                List<String> defs = new ArrayList<>();
                for(int j = 0; j < size; j++) {
                    defs.add(in.readUTF());
                }

                SlangEntry entry = new SlangEntry(id, word, defs);

                dic.put(key, entry);
            }
        } finally {
            in.close();
        }
        return new DictionaryService(dic);
    }

    public String  Normalize(String text) {
        return text.toLowerCase();
    }


}
