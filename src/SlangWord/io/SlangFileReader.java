package SlangWord.io;
import SlangWord.SlangEntry;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SlangFileReader {
   
    public  LoadResult readFile(String fileName) {
        LoadResult result = new LoadResult();
        BufferedReader br;
        Set<String> seenWords = new HashSet<>();
        try {
            int lineNumber = 0;
            br = new BufferedReader(new InputStreamReader( new FileInputStream(fileName), "UTF-8"));
            String str = br.readLine();
            if(str == null) {
                br.close();
                throw new IOException("file is empty");
            }
            while(true) {
                str = br.readLine();
                lineNumber++;
                
                // if(!seenWords.add(str)) {
                //     DataIssue is = new DataIssue(lineNumber, str, "duplicate slangword");
                //     result.addDataIssue(is);
                // }
                if(str == null) break;

                int pos = str.indexOf('`');
                if(pos == -1) {
                    DataIssue is = new DataIssue(lineNumber, str, "Missing separator");
                    result.addDataIssue(is);
                    continue;
                }
                String word = str.substring(0, pos).trim();

                String definitions = str.substring(pos+1).trim();

                if (word.isEmpty() || definitions.isEmpty()) {
                    result.addDataIssue(new DataIssue(
                        lineNumber, str, "Empty word or definition"
                    ));
                    continue;
                }

                if (!seenWords.add(word)) {
                    result.addDataIssue(new DataIssue(
                        lineNumber, str, "Duplicate slangword: " + word
                    ));
                }
                List<String> listDef = new ArrayList<>();


                for(String part : definitions.split("\\|")) {
                    listDef.add(part.trim());
                }

                if (listDef.isEmpty()) {
                    result.addDataIssue(new DataIssue(
                        lineNumber, str, "No valid definition"
                    ));
                    continue;
                }

                SlangEntry se = new SlangEntry(lineNumber, word, listDef );
                result.addEntry(se);




            }
            
            br.close();
            

        } catch(IOException e) {
            System.out.println("err =  " + e.toString());
        }


        return result;


    }
}
