package SlangWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizQuestion
{
    private final String prompt;
    private final List<String> choices;
    private final int correctIndex;

    public QuizQuestion(String prompt, List<String> choices, int correctIndex)
    {
        this.prompt = prompt;
        this.choices = Collections.unmodifiableList(new ArrayList<>(choices));
        this.correctIndex = correctIndex;
    }

    public String getPrompt()
    {
        return prompt;
    }

    public List<String> getChoices()
    {
        return choices;
    }

    public boolean isCorrect(int choiceIndex)
    {
        return choiceIndex == correctIndex;
    }

    public String getCorrectAnswer()
    {
        return choices.get(correctIndex);
    }
}