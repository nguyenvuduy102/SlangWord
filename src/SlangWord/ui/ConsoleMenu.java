package SlangWord.ui;

import SlangWord.SlangEntry;
import SlangWord.service.DictionaryService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

public class ConsoleMenu
{
    private final DictionaryService service;

    public ConsoleMenu(DictionaryService service)
    {
        this.service = service;
    }

    public void run() throws IOException
    {
        BufferedReader keyboard = new BufferedReader(
                new InputStreamReader(System.in));

        while (true)
        {
            printMenu();
            String choice = keyboard.readLine();

            if (choice == null)
            {
                return;
            }

            choice = choice.trim();

            switch (choice)
            {
                case "1":
                case "2":
                    System.out.print("Nhap tu khoa: ");
                    String keyword = keyboard.readLine();

                    if (keyword == null)
                    {
                        return;
                    }

                    if (keyword.trim().isEmpty())
                    {
                        System.out.println("Vui long nhap tu khoa.");
                        break;
                    }

                    showSearchResults(keyword, choice.equals("1"));
                    break;

                case "3":
                    showHistory();
                    break;

                case "4":  // TODO: Them slang, xu ly ghi de hoac tao ban trung.
                case "5":  // TODO: Sua slang.
                case "6":  // TODO: Xac nhan va xoa slang.
                case "7":  // TODO: Khoi phuc tu dien goc.
                case "8":  // TODO: Hien thi mot slang ngau nhien.
                case "9":  // TODO: Cho slang, chon 1 trong 4 nghia.
                case "10": // TODO: Cho nghia, chon 1 trong 4 slang.
                    System.out.println("Chuc nang nay chua duoc cai dat.");
                    break;

                case "0":
                    return;

                default:
                    System.out.println("Lua chon khong hop le.");
            }
        }
    }

    private void printMenu()
    {
        System.out.println("\n===== SLANG DICTIONARY =====");
        System.out.println("1.  Tim theo slang word");
        System.out.println("2.  Tim theo definition");
        System.out.println("3.  Xem lich su tra slang");
        System.out.println("4.  Them slang");
        System.out.println("5.  Sua slang");
        System.out.println("6.  Xoa slang");
        System.out.println("7.  Khoi phuc tu dien goc");
        System.out.println("8.  Hien thi slang ngau nhien");
        System.out.println("9.  Do vui: Cho slang, chon nghia");
        System.out.println("10. Do vui: Cho nghia, chon slang");
        System.out.println("0.  Thoat");
        System.out.print("Chon: ");
    }

    private void showSearchResults(String keyword, boolean searchByWord)
    {
        List<SlangEntry> results;
        long start = System.nanoTime();

        if (searchByWord)
        {
            SlangEntry entry = service.searchByWord(keyword);

            if (entry == null)
            {
                results = Collections.emptyList();
            }
            else
            {
                results = Collections.singletonList(entry);
            }
        }
        else
        {
            results = service.searchByDefinition(keyword);
        }

        double milliseconds = (System.nanoTime() - start) / 1_000_000.0;

        if (results.isEmpty())
        {
            System.out.println("Khong tim thay.");
        }
        else
        {
            System.out.println("So ket qua: " + results.size());

            for (SlangEntry entry : results)
            {
                System.out.println(entry);
            }
        }

        System.out.printf("Thoi gian tim: %.3f ms%n", milliseconds);
    }

    private void showHistory()
    {
        List<String> history = service.getSearchHistory();

        if (history.isEmpty())
        {
            System.out.println("Chua co lich su tra cuu.");
            return;
        }

        System.out.println("\n===== LICH SU TRA SLANG =====");

        for (int i = 0; i < history.size(); i++)
        {
            System.out.println((i + 1) + ". " + history.get(i));
        }
    }
}