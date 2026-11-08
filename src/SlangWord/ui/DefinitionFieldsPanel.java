package SlangWord.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

final class DefinitionFieldsPanel extends JPanel implements ActionListener {
    private static final int MINIMUM_FIELDS = 3;

    private final JPanel rows = new JPanel();
    private final List<JTextArea> fields = new ArrayList<>();

    DefinitionFieldsPanel(List<String> definitions) {
        super(new BorderLayout(0, 6));
        rows.setLayout(new BoxLayout(rows, BoxLayout.PAGE_AXIS));

        int count = Math.max(MINIMUM_FIELDS, definitions.size());
        for (int i = 0; i < count; i++) {
            addField(i < definitions.size() ? definitions.get(i) : "", false);
        }

        JScrollPane listScroll = new JScrollPane(rows,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.setPreferredSize(new Dimension(500, 192));
        listScroll.getVerticalScrollBar().setUnitIncrement(16);

        JButton add = new JButton("[+] Thêm ô nghĩa");
        add.setBackground(DictionaryPanel.BEIGE);
        add.setBorder(DictionaryPanel.raisedBorder(3, 8));
        add.addActionListener(this);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.add(add);

        add(new JLabel("Định nghĩa (nhập ít nhất một ô)"), BorderLayout.PAGE_START);
        add(listScroll, BorderLayout.CENTER);
        add(actions, BorderLayout.PAGE_END);
    }

    List<String> getDefinitions() {
        List<String> definitions = new ArrayList<>();

        for (JTextArea field : fields) {
            String definition = field.getText().trim();
            if (!definition.isEmpty()) {
                definitions.add(definition);
            }
        }

        return definitions;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        addField("", true);
    }

    private void addField(String value, boolean focus) {
        final JTextArea field = new JTextArea(value, 2, 32);
        field.setLineWrap(true);
        field.setWrapStyleWord(true);
        fields.add(field);

        JLabel label = new JLabel("Nghĩa " + fields.size());
        label.setLabelFor(field);

        JScrollPane input = new JScrollPane(field,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        input.setBorder(DictionaryPanel.sunkenBorder(1, 4));

        final JPanel row = new JPanel(new BorderLayout(0, 2));
        row.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        row.add(label, BorderLayout.PAGE_START);
        row.add(input, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        rows.add(row);

        if (focus) {
            rows.revalidate();
            rows.repaint();

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    rows.scrollRectToVisible(row.getBounds());
                    field.requestFocusInWindow();
                }
            });
        }
    }
}
