package SlangWord.ui;

import SlangWord.QuizQuestion;
import SlangWord.service.DictionaryService;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class QuizPanel extends JPanel implements ActionListener {
    private static final Color BLUE = new Color(0x255CA6);
    private static final Color SELECTED = new Color(0xE1EAF8);
    private static final Color CORRECT = new Color(0xE2F0E3);
    private static final Color INCORRECT = new Color(0xF6E1DF);

    private final DictionaryService service;
    private final JRadioButton wordMode = new JRadioButton(
            "Chế độ 1: Đoán Slang Word (Hiển thị 1 Slang Word → Chọn 1 trong 4 Definition đúng)", true);
    private final JRadioButton definitionMode = new JRadioButton(
            "Chế độ 2: Đoán Definition (Hiển thị 1 Definition → Chọn 1 trong 4 Slang Word đúng)");

    private final JLabel promptType = new JLabel();
    private final JTextPane promptText = new JTextPane();
    private final JLabel hint = new JLabel();
    private final JLabel feedback = new JLabel(" ");
    private final ButtonGroup answerGroup = new ButtonGroup();
    private final JToggleButton[] answers = new JToggleButton[4];
    private final JButton submit = createButton("[ Trả lời (Submit) ]", "SUBMIT");

    private QuizQuestion question;
    private int selected = -1;
    private boolean answered;

    public QuizPanel(DictionaryService service) {
        super(new BorderLayout(0, 7));
        this.service = service;
        add(createModePanel(), BorderLayout.PAGE_START);
        add(createQuestionPanel(), BorderLayout.CENTER);
        add(createAnswersPanel(), BorderLayout.PAGE_END);
    }

    private JPanel createModePanel() {
        JPanel panel = DictionaryPanel.createTitledPanel("Chế độ chơi (Game Mode Selection)");
        JPanel options = new JPanel(new GridLayout(2, 1, 0, 2));
        ButtonGroup modes = new ButtonGroup();

        for (JRadioButton mode : new JRadioButton[] { wordMode, definitionMode }) {
            modes.add(mode);
            mode.setActionCommand("MODE");
            mode.addActionListener(this);
            options.add(mode);
        }

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 3));
        actions.add(createButton("[ Làm mới câu hỏi ]", "REFRESH"));
        panel.add(options, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.LINE_END);
        return panel;
    }

    private JPanel createQuestionPanel() {
        JPanel panel = DictionaryPanel.createTitledPanel("Câu hỏi hiện tại");
        JPanel body = new JPanel(new BorderLayout(0, 9));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createCompoundBorder(
                DictionaryPanel.sunkenBorder(0, 0),
                BorderFactory.createEmptyBorder(8, 9, 8, 9)));

        promptType.setFont(promptType.getFont().deriveFont(Font.BOLD));
        JLabel topic = new JLabel("Chủ đề: Slang Word");
        topic.setOpaque(true);
        topic.setBackground(new Color(0xE8F0FF));
        topic.setForeground(DictionaryPanel.NAVY);
        topic.setFont(topic.getFont().deriveFont(Font.BOLD));
        topic.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x9AB8E4)),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DictionaryPanel.GRID),
                BorderFactory.createEmptyBorder(0, 0, 5, 0)));
        header.add(promptType, BorderLayout.CENTER);
        header.add(topic, BorderLayout.LINE_END);

        promptText.setEditable(false);
        promptText.setBackground(new Color(0xF2F5F8));
        promptText.setForeground(DictionaryPanel.NAVY);
        promptText.setBorder(BorderFactory.createEmptyBorder(16, 12, 10, 12));

        JScrollPane scroll = new JScrollPane(promptText);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xCBD5E2)));

        hint.setForeground(new Color(0x384C65));
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC));

        body.add(header, BorderLayout.PAGE_START);
        body.add(scroll, BorderLayout.CENTER);
        body.add(hint, BorderLayout.PAGE_END);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAnswersPanel() {
        JPanel panel = DictionaryPanel.createTitledPanel("Các phương án lựa chọn (Answers Panel)");
        JPanel grid = new JPanel(new GridLayout(2, 2, 8, 8));

        for (int i = 0; i < answers.length; i++) {
            JToggleButton answer = new JToggleButton();
            answer.setFont(UIManager.getFont("Button.font"));
            answer.setHorizontalAlignment(SwingConstants.LEFT);
            answer.setPreferredSize(new Dimension(270, 45));
            answer.setBorder(DictionaryPanel.raisedBorder(3, 8));
            answer.setBackground(DictionaryPanel.BEIGE);
            answer.setContentAreaFilled(false);
            answer.setOpaque(true);
            answer.setEnabled(false);
            answer.setActionCommand("ANSWER_" + i);
            answer.addActionListener(this);
            answerGroup.add(answer);
            answers[i] = answer;
            grid.add(answer);
        }

        submit.setEnabled(false);
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DictionaryPanel.GRID),
                BorderFactory.createEmptyBorder(7, 0, 0, 0)));
        footer.add(submit, BorderLayout.LINE_START);
        footer.add(feedback, BorderLayout.CENTER);
        panel.add(grid, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.PAGE_END);
        return panel;
    }

    private JButton createButton(String text, String command) {
        JButton button = new JButton(text);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setBackground(DictionaryPanel.BEIGE);
        button.setBorder(DictionaryPanel.raisedBorder(4, 10));
        button.setActionCommand(command);
        button.addActionListener(this);
        return button;
    }

    public void refreshQuestion() {
        boolean byWord = wordMode.isSelected();
        question = byWord ? service.createWordQuiz() : service.createDefinitionQuiz();
        selected = -1;
        answered = false;
        answerGroup.clearSelection();
        feedback.setText(" ");
        feedback.setToolTipText(null);
        submit.setEnabled(false);
        promptType.setText(byWord ? "SLANG WORD CẦN ĐOÁN:" : "DEFINITION CẦN ĐOÁN:");
        hint.setText(byWord ? "Gợi ý: Chọn một định nghĩa trong bốn đáp án rồi bấm [ Trả lời ]."
                : "Gợi ý: Chọn slang tương ứng với định nghĩa rồi bấm [ Trả lời ].");

        String prompt = question == null ? "Không đủ dữ liệu để tạo câu hỏi." : question.getPrompt();
        promptText.setText(prompt);

        SimpleAttributeSet format = new SimpleAttributeSet();
        StyleConstants.setAlignment(format, StyleConstants.ALIGN_CENTER);
        StyleConstants.setFontFamily(format, byWord && question != null ? Font.MONOSPACED : "Tahoma");
        StyleConstants.setFontSize(format, byWord && question != null ? 24 : 16);
        StyleConstants.setBold(format, true);
        StyleConstants.setForeground(format, DictionaryPanel.NAVY);
        promptText.getStyledDocument().setParagraphAttributes(0, prompt.length(), format, false);
        promptText.getStyledDocument().setCharacterAttributes(0, prompt.length(), format, false);
        promptText.setCaretPosition(0);

        updateAnswers();
    }

    private void updateAnswers() {
        for (int i = 0; i < answers.length; i++) {
            JToggleButton answer = answers[i];
            String value = question == null ? "" : question.getChoices().get(i);
            String prefix = "[ " + (char) ('A' + i) + " ]";
            String label = html(value);
            if (i == selected) {
                label = "<b>" + label + "</b>";
            }
            answer.setText("<html><b>" + prefix + "</b>&nbsp; " + label + "</html>");
            answer.setToolTipText(value.isEmpty() ? null
                    : "<html><body style='width: 330px'>" + html(value) + "</body></html>");
            answer.setEnabled(question != null && !answered);

            Color background = DictionaryPanel.BEIGE;
            Border border = DictionaryPanel.raisedBorder(3, 8);
            if (answered && question.isCorrect(i)) {
                background = CORRECT;
                border = highlightBorder(new Color(0x3D8250));
            } else if (answered && i == selected) {
                background = INCORRECT;
                border = highlightBorder(new Color(0xAC4A46));
            } else if (i == selected) {
                background = SELECTED;
                border = highlightBorder(BLUE);
            }

            answer.setBackground(background);
            answer.setBorder(border);
        }

        submit.setBackground(submit.isEnabled() ? SELECTED : DictionaryPanel.BEIGE);
        submit.setBorder(submit.isEnabled() ? highlightBorder(BLUE)
                : DictionaryPanel.raisedBorder(4, 10));
    }

    private Border highlightBorder(Color color) {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(3, 8, 3, 8));
    }

    private String html(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        switch (command) {
            case "MODE":
            case "REFRESH":
                refreshQuestion();
                break;
            case "SUBMIT":
                submitAnswer();
                break;
            case "ANSWER_0":
            case "ANSWER_1":
            case "ANSWER_2":
            case "ANSWER_3":
                selected = command.charAt(command.length() - 1) - '0';
                submit.setEnabled(true);
                updateAnswers();
                break;
        }
    }

    private void submitAnswer() {
        if (question == null || selected < 0 || answered) {
            return;
        }

        answered = true;
        boolean correct = question.isCorrect(selected);
        feedback.setForeground(correct ? new Color(0x235C32) : new Color(0x800000));
        String result = correct ? "Chính xác!" : "Đáp án đúng: " + question.getCorrectAnswer();
        feedback.setText(result);
        feedback.setToolTipText(result);
        submit.setEnabled(false);
        updateAnswers();
    }
}
