package SlangWord.ui;

import SlangWord.SlangEntry;
import SlangWord.service.DictionaryService;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class DictionaryPanel extends JPanel
        implements ActionListener, ListSelectionListener {
    public static final String DICTIONARY_TITLE =
            "Slang Word Dictionary v1.0 [Java Swing - Metal L&F]";
    private static final String HISTORY_TITLE =
            "Lịch sử tra cứu Slang Words - [Java Swing - Metal L&F] (Chức năng 3)";
    private static final String QUIZ_TITLE =
            "Slang Word Quiz Game - [Java Swing - Metal L&F] (Chức năng 9 & 10)";

    static final Color BEIGE = new Color(0xECE9D8);
    static final Color NAVY = new Color(0x0A246A);
    private static final Color EDGE = new Color(0x808080);
    static final Color GRID = new Color(0xD4D0C8);

    private final DictionaryService service;
    private final String sourceFile;
    private final String savedFile;
    private final CardLayout viewLayout = new CardLayout();
    private final JPanel views = new JPanel(viewLayout);
    private final SearchHistoryPanel historyPanel;
    private final QuizPanel quizPanel;

    private final JTextField keywordField = new JTextField() {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            if (getText().isEmpty() && !isFocusOwner()) {
                graphics.setColor(new Color(0x666666));
                graphics.drawString("Nhập từ hoặc định nghĩa...", getInsets().left + 2,
                        (getHeight() - graphics.getFontMetrics().getHeight()) / 2
                                + graphics.getFontMetrics().getAscent());
            }
        }
    };
    private final JRadioButton wordMode = new JRadioButton("Tìm theo Slang Word", true);
    private final JRadioButton definitionMode = new JRadioButton("Tìm theo Definition (Định nghĩa)");

    private final JLabel randomWord = new JLabel();
    private final JTextArea randomDefinitions = new JTextArea(2, 12);
    private final JTextField detailWord = new JTextField();
    private final JTextArea detailDefinitions = new JTextArea(6, 16);
    private final JLabel pageLabel = new JLabel();
    private final JLabel resultCount = new JLabel();
    private final JComboBox<Integer> pageSizeBox = new JComboBox<>(
            new Integer[] { 10, 25, 50, 100 });

    private final JButton allButton = createButton("Tất cả Slang", "ALL");
    private final JButton historyButton = createButton("Lịch sử tìm kiếm", "HISTORY");
    private final JButton quizButton = createButton("[Game] Đố vui Slang Word", "QUIZ", true);
    private final JButton editButton = createButton("[Chỉnh sửa]", "EDIT");
    private final JButton deleteButton = createButton("[Xóa từ này]", "DELETE");
    private final JButton firstButton = createButton("[|<]", "FIRST");
    private final JButton previousButton = createButton("[<]", "PREVIOUS");
    private final JButton nextButton = createButton("[>]", "NEXT");
    private final JButton lastButton = createButton("[>|]", "LAST");
    private final JButton copyButton = createButton("[Sao chép]", "COPY");

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "STT", "Slang Word", "Định nghĩa (Definition)" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);
    private List<SlangEntry> results = Collections.emptyList();
    private List<SlangEntry> visibleEntries = Collections.emptyList();
    private int page;
    private int pageCount = 1;
    private String randomTerm = "";

    public DictionaryPanel(DictionaryService service, String savedFile, String sourceFile) {
        super(new BorderLayout());
        this.service = service;
        this.savedFile = savedFile;
        this.sourceFile = sourceFile;

        setPreferredSize(new Dimension(1340, 550));

        JPanel dictionary = new JPanel(new BorderLayout(6, 0));
        dictionary.add(createWorkspace(), BorderLayout.CENTER);
        dictionary.add(createInspector(), BorderLayout.LINE_END);
        historyPanel = new SearchHistoryPanel(service);
        quizPanel = new QuizPanel(service);
        views.add(dictionary, "DICTIONARY");
        views.add(historyPanel, "HISTORY");
        views.add(quizPanel, "QUIZ");

        JPanel body = new JPanel(new BorderLayout(6, 0));
        body.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
        body.add(createNavigation(), BorderLayout.LINE_START);
        body.add(views, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        registerKeyboardAction(this, "ALL", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        showAll();
        showRandom();
    }

    public static void applyClassicStyle() {
        MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());

        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            throw new IllegalStateException("Không mở được giao diện Metal.", e);
        }

        FontUIResource font = new FontUIResource("Tahoma", Font.PLAIN, 12);
        String[] components = { "Label", "Button", "RadioButton", "TextField",
                "TextArea", "Table", "TableHeader", "ComboBox", "List", "OptionPane" };

        for (String component : components) {
            UIManager.put(component + ".font", font);
        }

        UIManager.put("Panel.background", BEIGE);
        UIManager.put("ToolBar.background", BEIGE);
        UIManager.put("RadioButton.background", BEIGE);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("activeCaption", new Color(0x3A6EA5));
        UIManager.put("activeCaptionText", Color.WHITE);
        UIManager.put("activeCaptionBorder", new Color(0x204060));
        UIManager.put("InternalFrame.titleFont", font.deriveFont(Font.BOLD));
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, EDGE),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));

        JLabel grip = new JLabel("::: ");
        grip.setForeground(EDGE);
        toolbar.add(grip);
        toolbar.add(createButton("[+] Thêm từ mới", "ADD", true));
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(createButton("[R] Khôi phục gốc (slang.txt)", "RESET"));
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(createButton("[F5] Làm mới", "ALL"));
        toolbar.addSeparator();
        toolbar.add(createButton("[?] Hướng dẫn", "HELP"));
        return toolbar;
    }

    private JPanel createNavigation() {
        JPanel navigation = createTitledPanel("Danh mục");
        navigation.setPreferredSize(new Dimension(224, 0));

        allButton.setFont(allButton.getFont().deriveFont(Font.BOLD));
        allButton.setForeground(NAVY);
        quizButton.setForeground(NAVY);

        JPanel links = new JPanel(new GridLayout(3, 1, 0, 5));

        for (JButton link : new JButton[] { allButton, historyButton, quizButton }) {
            link.setHorizontalAlignment(SwingConstants.LEFT);
            links.add(link);
        }

        navigation.add(links, BorderLayout.PAGE_START);
        return navigation;
    }

    private JPanel createWorkspace() {
        JPanel top = new JPanel(new GridLayout(0, 1, 0, 5));
        top.add(createRandomPanel());
        top.add(createSearchPanel());

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.add(createToolbar(), BorderLayout.PAGE_START);
        header.add(top, BorderLayout.CENTER);

        JPanel workspace = new JPanel(new BorderLayout(0, 5));
        workspace.add(header, BorderLayout.PAGE_START);
        workspace.add(createTablePanel(), BorderLayout.CENTER);
        return workspace;
    }

    private JPanel createRandomPanel() {
        JPanel panel = createTitledPanel("On This Day - Slang Word Ngẫu Nhiên");
        randomWord.setForeground(NAVY);
        randomWord.setFont(randomWord.getFont().deriveFont(Font.BOLD, 14f));

        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        title.add(randomWord);
        title.add(new JLabel("   [English Slang]"));

        configureTextArea(randomDefinitions);
        randomDefinitions.setOpaque(false);

        JPanel description = new JPanel(new BorderLayout(0, 2));
        description.add(title, BorderLayout.PAGE_START);
        description.add(randomDefinitions, BorderLayout.CENTER);
        panel.add(description, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.add(createButton("[Từ ngẫu nhiên khác]", "RANDOM"));
        actions.add(copyButton);
        panel.add(actions, BorderLayout.LINE_END);

        return panel;
    }

    private JPanel createSearchPanel() {
        JPanel panel = createTitledPanel("Tìm kiếm Slang Words");

        JLabel keywordLabel = new JLabel("Nhập từ khóa:");
        keywordLabel.setFont(keywordLabel.getFont().deriveFont(Font.BOLD));
        keywordLabel.setLabelFor(keywordField);
        keywordField.setBorder(sunkenBorder(1, 6));
        keywordField.setActionCommand("SEARCH");
        keywordField.addActionListener(this);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actions.add(createButton("[ Tìm kiếm ]", "SEARCH", true));
        actions.add(createButton("[ Xóa trắng ]", "ALL"));

        JPanel input = new JPanel(new BorderLayout(7, 0));
        input.add(keywordLabel, BorderLayout.LINE_START);
        input.add(keywordField, BorderLayout.CENTER);
        input.add(actions, BorderLayout.LINE_END);

        ButtonGroup modes = new ButtonGroup();
        modes.add(wordMode);
        modes.add(definitionMode);
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 1));
        modePanel.add(wordMode);
        modePanel.add(definitionMode);

        panel.add(input, BorderLayout.PAGE_START);
        panel.add(modePanel, BorderLayout.PAGE_END);
        return panel;
    }

    private JPanel createTablePanel() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(21);
        table.setGridColor(GRID);
        table.setShowGrid(true);
        table.setSelectionBackground(NAVY);
        table.setSelectionForeground(Color.WHITE);
        table.getSelectionModel().addListSelectionListener(this);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setMinWidth(40);
        table.getColumnModel().getColumn(0).setMaxWidth(44);
        table.getColumnModel().getColumn(1).setPreferredWidth(112);
        table.getColumnModel().getColumn(2).setPreferredWidth(590);

        DefaultTableCellRenderer numberCell = new DefaultTableCellRenderer();
        numberCell.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(numberCell);
        DefaultTableCellRenderer wordCell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focused,
                        row, column);
                setFont(source.getFont().deriveFont(Font.BOLD));
                return this;
            }
        };
        table.getColumnModel().getColumn(1).setCellRenderer(wordCell);

        applyTableHeader(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(sunkenBorder(0, 0));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pageLabel.setFont(pageLabel.getFont().deriveFont(Font.BOLD));
        pager.add(new JLabel("Trang:"));
        pager.add(firstButton);
        pager.add(previousButton);
        pager.add(pageLabel);
        pager.add(nextButton);
        pager.add(lastButton);

        pageSizeBox.setSelectedItem(50);
        pageSizeBox.setActionCommand("PAGE_SIZE");
        pageSizeBox.addActionListener(this);

        JPanel counts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        counts.add(new JLabel("Hiển thị:"));
        counts.add(pageSizeBox);
        counts.add(new JLabel("/trang  |"));
        counts.add(resultCount);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(pager, BorderLayout.LINE_START);
        footer.add(counts, BorderLayout.LINE_END);

        JPanel panel = createTitledPanel("Danh sách từ điển (JTable / DefaultTableModel)");
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.PAGE_END);
        return panel;
    }

    static void applyTableHeader(JTable table) {
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focused,
                        row, column);
                setBackground(new Color(0xE1DFD5));
                setFont(source.getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                setBorder(raisedBorder(3, 5));
                return this;
            }
        });
    }

    private JPanel createInspector() {
        JPanel inspector = createTitledPanel("Chi tiết Slang Word được chọn");
        inspector.setPreferredSize(new Dimension(255, 0));

        detailWord.setEditable(false);
        detailWord.setBackground(Color.WHITE);
        detailWord.setForeground(NAVY);
        detailWord.setFont(detailWord.getFont().deriveFont(Font.BOLD, 14f));
        detailWord.setBorder(sunkenBorder(5, 7));
        configureTextArea(detailDefinitions);
        detailDefinitions.setBackground(Color.WHITE);
        detailDefinitions.setMargin(new java.awt.Insets(5, 7, 5, 7));

        JPanel wordPanel = new JPanel(new BorderLayout(0, 2));
        wordPanel.add(new JLabel("Slang Word:"), BorderLayout.PAGE_START);
        wordPanel.add(detailWord, BorderLayout.CENTER);

        JScrollPane definitionScroll = new JScrollPane(detailDefinitions);
        definitionScroll.setBorder(sunkenBorder(0, 0));
        JPanel definitionPanel = new JPanel(new BorderLayout(0, 2));
        definitionPanel.add(new JLabel("Definition (Định nghĩa):"), BorderLayout.PAGE_START);
        definitionPanel.add(definitionScroll, BorderLayout.CENTER);

        JPanel fields = new JPanel(new BorderLayout(0, 8));
        fields.add(wordPanel, BorderLayout.PAGE_START);
        fields.add(definitionPanel, BorderLayout.CENTER);
        inspector.add(fields, BorderLayout.PAGE_START);

        editButton.setFont(editButton.getFont().deriveFont(Font.BOLD));
        deleteButton.setFont(deleteButton.getFont().deriveFont(Font.BOLD));
        deleteButton.setForeground(new Color(0x800000));

        JPanel actions = new JPanel(new BorderLayout(5, 0));
        actions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EDGE),
                BorderFactory.createEmptyBorder(7, 0, 0, 0)));
        actions.add(editButton, BorderLayout.CENTER);
        actions.add(deleteButton, BorderLayout.LINE_END);
        inspector.add(actions, BorderLayout.PAGE_END);
        return inspector;
    }

    static JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(createTitledBorder(title),
                BorderFactory.createEmptyBorder(2, 3, 3, 3)));
        return panel;
    }

    static TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(Color.WHITE, EDGE), title);
        border.setTitleFont(new Font("Tahoma", Font.BOLD, 12));
        return border;
    }

    static Border raisedBorder(int vertical, int horizontal) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.RAISED,
                        Color.WHITE, BEIGE, new Color(0x404040), EDGE),
                BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal));
    }

    static Border sunkenBorder(int vertical, int horizontal) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED,
                        Color.WHITE, BEIGE, EDGE, GRID),
                BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal));
    }

    private JButton createButton(String text, String command) {
        return createButton(text, command, false);
    }

    private JButton createButton(String text, String command, boolean bold) {
        JButton button = new JButton(text);
        button.setActionCommand(command);
        button.addActionListener(this);
        button.setBackground(BEIGE);
        button.setBorder(raisedBorder(2, 7));

        if (bold) {
            button.setFont(button.getFont().deriveFont(Font.BOLD));
        }

        return button;
    }

    private void configureTextArea(JTextArea area) {
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        switch (event.getActionCommand()) {
            case "ALL":
                showAll();
                break;
            case "SEARCH":
                search();
                break;
            case "RANDOM":
                showRandom();
                break;
            case "COPY":
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection(randomTerm), null);
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
            case "ADD":
                showEntryDialog(null);
                break;
            case "EDIT":
                SlangEntry selected = selectedEntry();
                if (selected != null) {
                    showEntryDialog(selected);
                }
                break;
            case "DELETE":
                deleteSelected();
                break;
            case "RESET":
                resetDictionary();
                break;
            case "HISTORY":
                showHistory();
                break;
            case "QUIZ":
                showQuiz();
                break;
            case "HELP":
                JOptionPane.showMessageDialog(this,
                        "Nhập từ khóa và chọn kiểu tìm kiếm.\n"
                                + "Chọn một hàng để xem nghĩa, chỉnh sửa hoặc xóa.\n"
                                + "Nhấn F5 để trở về danh sách đầy đủ.",
                        "Hướng dẫn", JOptionPane.INFORMATION_MESSAGE);
                break;
        }
    }

    private void showAll() {
        showView("DICTIONARY", DICTIONARY_TITLE);
        keywordField.setText("");
        results = service.getAllEntries();
        allButton.setText("Tất cả Slang (" + formatCount(results.size()) + ")");
        refreshHistoryLabel();
        showPage(0);
    }

    private void search() {
        String keyword = keywordField.getText().trim();

        if (keyword.isEmpty()) {
            showAll();
            return;
        }

        if (wordMode.isSelected()) {
            results = service.searchByWord(keyword);
        } else {
            results = service.searchByDefinition(keyword);
        }

        allButton.setBackground(BEIGE);
        refreshHistoryLabel();
        showPage(0);

        String error = service.getSearchHistoryError();
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Lịch sử tra cứu",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showRandom() {
        List<SlangEntry> entries = service.randomSlang();
        randomTerm = entries.isEmpty() ? "" : entries.get(0).getWord();
        copyButton.setEnabled(!randomTerm.isEmpty());
        randomWord.setText(entries.isEmpty() ? "Từ điển rỗng" : randomTerm);

        List<String> definitions = new ArrayList<>();
        for (SlangEntry entry : entries) {
            definitions.addAll(entry.getDefinitions());
        }

        String text = entries.isEmpty() ? "" : "Định nghĩa: " + String.join("; ", definitions);
        randomDefinitions.setText(text);
        randomDefinitions.setToolTipText(text);
    }

    private String formatCount(int count) {
        return String.format(Locale.US, "%,d", count);
    }

    private void showPage(int requestedPage) {
        int pageSize = (Integer) pageSizeBox.getSelectedItem();
        pageCount = Math.max(1, (results.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(requestedPage, pageCount - 1));

        int start = page * pageSize;
        int end = Math.min(start + pageSize, results.size());

        table.clearSelection();
        model.setRowCount(0);
        visibleEntries = results.subList(start, end);

        for (int i = 0; i < visibleEntries.size(); i++) {
            SlangEntry entry = visibleEntries.get(i);
            model.addRow(new Object[] {
                    start + i + 1, entry.getWord(),
                    String.join("; ", entry.getDefinitions())
            });
        }

        pageLabel.setText("Trang " + (page + 1) + " / " + pageCount);
        resultCount.setText("Tổng cộng: " + formatCount(results.size()) + " từ");
        firstButton.setEnabled(page > 0);
        previousButton.setEnabled(page > 0);
        nextButton.setEnabled(page + 1 < pageCount);
        lastButton.setEnabled(page + 1 < pageCount);

        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
            table.scrollRectToVisible(table.getCellRect(0, 0, true));
        }

        showDetails();
    }

    private SlangEntry selectedEntry() {
        int row = table.getSelectedRow();
        return row < 0 ? null : visibleEntries.get(table.convertRowIndexToModel(row));
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (!event.getValueIsAdjusting()) {
            showDetails();
        }
    }

    private void showDetails() {
        SlangEntry entry = selectedEntry();
        editButton.setEnabled(entry != null);
        deleteButton.setEnabled(entry != null);

        detailWord.setText(entry == null ? "" : entry.getWord());
        detailWord.setToolTipText(entry == null ? null : "ID " + entry.getId());
        detailDefinitions.setText(entry == null ? "" : String.join("\n", entry.getDefinitions()));
        detailDefinitions.setCaretPosition(0);
    }

    private void refreshHistoryLabel() {
        historyButton.setText("Lịch sử tìm kiếm (" + formatCount(service.getSearchHistoryCount()) + ")");
    }

    private void showHistory() {
        refreshHistoryLabel();
        historyPanel.refresh();
        showView("HISTORY", HISTORY_TITLE);
    }

    private void showQuiz() {
        refreshHistoryLabel();
        quizPanel.refreshQuestion();
        showView("QUIZ", QUIZ_TITLE);
    }

    private void showView(String view, String title) {
        viewLayout.show(views, view);
        boolean dictionary = "DICTIONARY".equals(view);
        boolean history = "HISTORY".equals(view);
        boolean quiz = "QUIZ".equals(view);

        allButton.setBackground(dictionary ? new Color(0xDCD8CB) : BEIGE);
        historyButton.setBackground(history ? new Color(0xC7D7EC) : BEIGE);
        historyButton.setForeground(history ? NAVY : Color.BLACK);
        historyButton.setFont(historyButton.getFont().deriveFont(history ? Font.BOLD : Font.PLAIN));
        quizButton.setBackground(quiz ? new Color(0xDCD8CB) : BEIGE);
        quizButton.setBorder(quiz ? sunkenBorder(2, 7) : raisedBorder(2, 7));
        updateTitle(title);
    }

    private void updateTitle(String title) {
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame) {
            ((JFrame) window).setTitle(title);
        }
    }

    private void showEntryDialog(SlangEntry current) {
        String title = current == null ? "Thêm từ mới" : "Chỉnh sửa slang";
        JTextField wordField = new JTextField(current == null ? "" : current.getWord(), 24);
        List<String> initialDefinitions = current == null
                ? Collections.emptyList() : current.getDefinitions();
        DefinitionFieldsPanel meaningInput = new DefinitionFieldsPanel(initialDefinitions);

        JPanel form = new JPanel(new BorderLayout(0, 9));
        JPanel wordInput = new JPanel(new BorderLayout(0, 2));
        wordInput.add(new JLabel("Slang Word"), BorderLayout.PAGE_START);
        wordInput.add(wordField, BorderLayout.CENTER);
        form.add(wordInput, BorderLayout.PAGE_START);
        form.add(meaningInput, BorderLayout.CENTER);

        while (JOptionPane.showConfirmDialog(this, form, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String word = wordField.getText().trim();
            List<String> definitions = meaningInput.getDefinitions();

            if (word.isEmpty() || definitions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cần nhập slang và ít nhất một nghĩa.");
                continue;
            }

            try {
                List<SlangEntry> existing = service.findByWord(word);

                if (current == null) {
                    Long overwriteId = null;

                    if (!existing.isEmpty()) {
                        Object[] options = { "Ghi đè", "Tạo bản trùng", "Hủy" };
                        int choice = JOptionPane.showOptionDialog(this,
                                "Slang \"" + word + "\" đã có trong từ điển.", title,
                                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                                null, options, options[0]);

                        if (choice == 0) {
                            SlangEntry selected = chooseOverwriteEntry(existing);
                            if (selected == null) {
                                return;
                            }
                            overwriteId = selected.getId();
                        } else if (choice != 1) {
                            return;
                        }
                    }

                    service.addWord(word, definitions, overwriteId, savedFile);
                } else {
                    if (!service.Normalize(word).equals(service.Normalize(current.getWord()))
                            && !existing.isEmpty()
                            && !confirm("Tên mới đã tồn tại. Bạn muốn tiếp tục?")) {
                        return;
                    }

                    service.editWord(current.getId(), word, definitions, savedFile);
                }

                showAll();
                showRandom();
                return;
            } catch (IOException | IllegalArgumentException e) {
                showError("Không lưu được", e);
                return;
            }
        }
    }

    private SlangEntry chooseOverwriteEntry(List<SlangEntry> entries) {
        if (entries.size() == 1) {
            return entries.get(0);
        }

        return (SlangEntry) JOptionPane.showInputDialog(this,
                "Chọn bản ghi cần ghi đè theo ID.", "Ghi đè slang",
                JOptionPane.QUESTION_MESSAGE, null, entries.toArray(), entries.get(0));
    }

    private void deleteSelected() {
        SlangEntry entry = selectedEntry();

        if (entry == null || !DeleteConfirmation.show(this, entry)) {
            return;
        }

        try {
            service.deleteWord(entry.getId(), savedFile);
            showAll();
            showRandom();
        } catch (IOException | IllegalArgumentException e) {
            showError("Không xóa được", e);
        }
    }

    private void resetDictionary() {
        if (!confirm("Khôi phục dữ liệu từ slang.txt?\nCác thay đổi thêm, sửa và xóa sẽ bị bỏ.")) {
            return;
        }

        try {
            int restored = service.resetToOriginal(sourceFile, savedFile);
            showAll();
            showRandom();
            JOptionPane.showMessageDialog(this, "Đã khôi phục " + formatCount(restored) + " bản ghi.");
        } catch (IOException e) {
            showError("Không khôi phục được", e);
        }
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void showError(String title, Exception error) {
        JOptionPane.showMessageDialog(this, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}
