package SlangWord;

import SlangWord.io.LoadResult;
import SlangWord.io.SlangFileReader;
import SlangWord.service.DictionaryService;
import SlangWord.ui.DictionaryPanel;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    private static void createAndShowGUI(DictionaryService service,
            String savedFile, String sourceFile) {
        DictionaryPanel.applyClassicStyle();
        JFrame.setDefaultLookAndFeelDecorated(true);

        JFrame frame = new JFrame("Slang Word Dictionary v1.0 [Java Swing - Metal L&F]");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new DictionaryPanel(service, savedFile, sourceFile));
        frame.pack();

        Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        frame.setSize(Math.min(frame.getWidth(), available.width),
                Math.min(frame.getHeight(), available.height));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        String sourceFile = "data/slang.txt";
        String savedFile = "data/dictionary-word-v2.dat";

        try {
            DictionaryService service;

            if (new File(savedFile).exists()) {
                service = DictionaryService.load(savedFile);
                System.out.println("Da tai chi muc.");
            } else {
                SlangFileReader reader = new SlangFileReader();
                LoadResult result = reader.readFile(sourceFile);

                if (result.getEntris().isEmpty()) {
                    System.out.println("Khong co du lieu hop le.");
                    return;
                }

                service = new DictionaryService(result.getEntris());
                service.save(savedFile);
                System.out.println("Da tao va luu chi muc.");
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    createAndShowGUI(service, savedFile, sourceFile);
                }
            });
        } catch (IOException e) {
            System.out.println("Loi: " + e.getMessage());
        }
    }
}
