package SlangWord.ui;

import SlangWord.SlangEntry;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;

final class DeleteConfirmation {
    private static final String DELETE = "Xóa";
    private static final String CANCEL = "Hủy bỏ";

    private DeleteConfirmation() {
    }

    static boolean show(JComponent owner, SlangEntry entry) {
        final JOptionPane pane = createPane(entry);
        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(owner),
                "Xác nhận xóa - [JOptionPane]", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(pane);
        dialog.setResizable(false);

        pane.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (JOptionPane.VALUE_PROPERTY.equals(event.getPropertyName())
                        && event.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
                    dialog.dispose();
                }
            }
        });

        dialog.getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                pane.setValue(CANCEL);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        pane.selectInitialValue();
        dialog.setVisible(true);
        return DELETE.equals(pane.getValue());
    }

    static JOptionPane createPane(SlangEntry entry) {
        String word = entry.getWord().replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;");
        JLabel heading = new JLabel("<html><div style='width: 305px'><b>"
                + "Bạn có chắc chắn muốn xóa slang word "
                + "<span style='color:#A02724; background-color:#F4DDDA'>\""
                + word + "\"</span> khỏi từ điển không?</b></div></html>");
        heading.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DictionaryPanel.GRID));
        JLabel note = new JLabel("<html><div style='width: 305px'>"
                + "Thao tác này sẽ xóa bản ghi khỏi từ điển và lưu thay đổi vào file. "
                + "Sau khi lưu, bạn không thể hoàn tác.</div></html>");
        note.setFont(note.getFont().deriveFont(11f));

        JTextArea details = new JTextArea("Mã từ: #" + entry.getId()
                + " [" + entry.getWord() + "]\nĐịnh nghĩa: "
                + String.join("; ", entry.getDefinitions()));
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
        details.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(details,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(305, 49));
        scroll.setBorder(DictionaryPanel.sunkenBorder(0, 0));

        JPanel message = new JPanel(new BorderLayout(0, 5));
        message.add(heading, BorderLayout.PAGE_START);
        message.add(note, BorderLayout.CENTER);
        message.add(scroll, BorderLayout.PAGE_END);

        Object[] options = { DELETE, "Không", CANCEL };
        JOptionPane pane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options, CANCEL);
        pane.setBackground(DictionaryPanel.BEIGE);
        return pane;
    }
}
