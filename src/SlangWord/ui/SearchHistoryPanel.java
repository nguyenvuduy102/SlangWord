package SlangWord.ui;

import SlangWord.SearchHistoryEntry;
import SlangWord.service.DictionaryService;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class SearchHistoryPanel extends JPanel implements ActionListener {
    private final DictionaryService service;

    private final JRadioButton all = new JRadioButton("Tất cả", true);
    private final JRadioButton words = new JRadioButton("Tra cứu theo Slang Word");
    private final JRadioButton definitions = new JRadioButton("Tra cứu theo Definition");
    private final JComboBox<Integer> pageSize = new JComboBox<>(new Integer[] { 10, 15, 25, 50 });

    private final JLabel pageLabel = new JLabel();
    private final JLabel countLabel = new JLabel();
    private final JLabel errorLabel = new JLabel();
    private final JButton first = createButton("[|<]", "FIRST");
    private final JButton previous = createButton("[<]", "PREVIOUS");
    private final JButton next = createButton("[>]", "NEXT");
    private final JButton last = createButton("[>|]", "LAST");
    private final TitledBorder tableTitle = DictionaryPanel.createTitledBorder("Bảng Lịch sử Tra cứu");

    private final DefaultTableModel model = new DefaultTableModel(new Object[] {
            "STT", "Thời điểm tra", "Từ khóa tra cứu", "Hình thức tra",
            "Kết quả", "Định nghĩa tương ứng (Definition)"
    }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);
    private final List<SearchHistoryEntry> filtered = new ArrayList<>();
    private int page;
    private int pageCount = 1;

    public SearchHistoryPanel(DictionaryService service) {
        super(new BorderLayout(0, 6));
        this.service = service;

        add(createFilters(), BorderLayout.PAGE_START);
        add(createTablePanel(), BorderLayout.CENTER);

        errorLabel.setForeground(new Color(0x800000));
        errorLabel.setVisible(false);
        add(errorLabel, BorderLayout.PAGE_END);
    }

    private JPanel createFilters() {
        JPanel filters = DictionaryPanel.createTitledPanel(
                "Bộ lọc & Tra cứu trong Lịch sử (Search in History)");
        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        JLabel label = new JLabel("Chế độ lọc:");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        modes.add(label);

        ButtonGroup group = new ButtonGroup();
        for (JRadioButton radio : new JRadioButton[] { all, words, definitions }) {
            group.add(radio);
            modes.add(radio);
            radio.setActionCommand("FILTER");
            radio.addActionListener(this);
        }

        filters.add(modes, BorderLayout.CENTER);
        return filters;
    }

    private JPanel createTablePanel() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(21);
        table.setGridColor(DictionaryPanel.GRID);
        table.setShowGrid(true);
        table.setSelectionBackground(DictionaryPanel.NAVY);
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);
        DictionaryPanel.applyTableHeader(table);

        int[] widths = { 40, 130, 120, 112, 90, 490 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(44);
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centered);
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focused, row, column);
                setFont(source.getFont().deriveFont(Font.BOLD));
                return this;
            }
        });
        table.getColumnModel().getColumn(3).setCellRenderer(new ModeRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focused, row, column);
                setToolTipText(value == null ? null : value.toString());
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(DictionaryPanel.sunkenBorder(0, 0));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pageLabel.setFont(pageLabel.getFont().deriveFont(Font.BOLD));
        pager.add(first);
        pager.add(previous);
        pager.add(pageLabel);
        pager.add(next);
        pager.add(last);

        pageSize.setSelectedItem(15);
        pageSize.setActionCommand("PAGE_SIZE");
        pageSize.addActionListener(this);

        JPanel counts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        counts.add(new JLabel("Hiển thị:"));
        counts.add(pageSize);
        counts.add(new JLabel("/ trang  |"));
        counts.add(countLabel);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(pager, BorderLayout.LINE_START);
        footer.add(counts, BorderLayout.LINE_END);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(tableTitle,
                BorderFactory.createEmptyBorder(2, 3, 3, 3)));
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.PAGE_END);
        return panel;
    }

    private JButton createButton(String text, String command) {
        JButton button = new JButton(text);
        button.setActionCommand(command);
        button.addActionListener(this);
        button.setBackground(DictionaryPanel.BEIGE);
        button.setBorder(DictionaryPanel.raisedBorder(2, 7));
        return button;
    }

    public void refresh() {
        List<SearchHistoryEntry> history = service.getSearchHistory();
        SearchHistoryEntry.Mode mode = null;

        if (words.isSelected()) {
            mode = SearchHistoryEntry.Mode.WORD;
        } else if (definitions.isSelected()) {
            mode = SearchHistoryEntry.Mode.DEFINITION;
        }

        filtered.clear();
        for (int i = history.size() - 1; i >= 0; i--) {
            SearchHistoryEntry entry = history.get(i);
            if (mode == null || entry.getMode() == mode) {
                filtered.add(entry);
            }
        }

        String error = service.getSearchHistoryError();
        errorLabel.setText(error);
        errorLabel.setVisible(error != null);

        String count = String.format(Locale.US, "%,d", filtered.size());
        tableTitle.setTitle("Bảng Lịch sử Tra cứu (JTable / DefaultTableModel - "
                + count + " bản ghi)");
        countLabel.setText("Tổng cộng: " + count + " lượt tra cứu");
        showPage(0);
        repaint();
    }

    private void showPage(int requestedPage) {
        int size = (Integer) pageSize.getSelectedItem();
        pageCount = Math.max(1, (filtered.size() + size - 1) / size);
        page = Math.max(0, Math.min(requestedPage, pageCount - 1));

        int start = page * size;
        int end = Math.min(start + size, filtered.size());

        model.setRowCount(0);
        for (int i = start; i < end; i++) {
            SearchHistoryEntry entry = filtered.get(i);
            model.addRow(new Object[] { i + 1, entry.getFormattedTime(),
                    entry.getKeyword(), entry.getMode(), entry.getResultLabel(),
                    entry.getDetails().isEmpty() ? "—" : entry.getDetails() });
        }

        pageLabel.setText("Trang " + (page + 1) + " / " + pageCount);
        first.setEnabled(page > 0);
        previous.setEnabled(page > 0);
        next.setEnabled(page + 1 < pageCount);
        last.setEnabled(page + 1 < pageCount);

        if (model.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
            table.scrollRectToVisible(table.getCellRect(0, 0, true));
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        switch (event.getActionCommand()) {
            case "FILTER":
                refresh();
                break;
            case "PAGE_SIZE":
            case "FIRST":
                showPage(0);
                break;
            case "PREVIOUS":
                showPage(page - 1);
                break;
            case "NEXT":
                showPage(page + 1);
                break;
            case "LAST":
                showPage(pageCount - 1);
                break;
        }
    }

    private static class ModeRenderer extends DefaultTableCellRenderer {
        private boolean definition;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            SearchHistoryEntry.Mode mode = (SearchHistoryEntry.Mode) value;
            definition = mode == SearchHistoryEntry.Mode.DEFINITION;
            setText(mode.getLabel());
            setFont(table.getFont().deriveFont(10f));
            return this;
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
            graphics.setFont(getFont());

            java.awt.FontMetrics metrics = graphics.getFontMetrics();
            int badgeWidth = metrics.stringWidth(getText()) + 10;
            int badgeHeight = metrics.getHeight() + 2;
            int top = (getHeight() - badgeHeight) / 2;

            graphics.setColor(new Color(definition ? 0xE7F5EA : 0xE8F0FF));
            graphics.fillRect(5, top, badgeWidth, badgeHeight);
            graphics.setColor(new Color(definition ? 0x70B783 : 0x7B9BD9));
            graphics.drawRect(5, top, badgeWidth - 1, badgeHeight - 1);
            graphics.setColor(new Color(definition ? 0x145223 : 0x0A246A));
            graphics.drawString(getText(), 10, top + 1 + metrics.getAscent());
        }
    }
}
