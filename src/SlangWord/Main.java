package SlangWord;

import SlangWord.io.LoadResult;
import SlangWord.io.SlangFileReader;
import SlangWord.service.DictionaryService;
import SlangWord.ui.ConsoleMenu;

import java.io.File;
import java.io.IOException;

public class Main
{
    public static void main(String[] args)
    {
        String sourceFile = "data/slang.txt";

        // Dung ten moi de khong nham voi file luu theo cau truc cu.
        String savedFile = "data/dictionary-word-v1.dat";

        try
        {
            DictionaryService service;

            if (new File(savedFile).exists())
            {
                service = DictionaryService.load(savedFile);
                System.out.println("Da tai chi muc.");
            }
            else
            {
                SlangFileReader reader = new SlangFileReader();
                LoadResult result = reader.readFile(sourceFile);

                if (result.getEntris().isEmpty())
                {
                    System.out.println("Khong co du lieu hop le.");
                    return;
                }

                service = new DictionaryService(result.getEntris());
                service.save(savedFile);

                System.out.println("Da tao va luu chi muc.");
            }

            new ConsoleMenu(service).run();
        }
        catch (IOException e)
        {
            System.out.println("Loi: " + e.getMessage());
        }
    }
}