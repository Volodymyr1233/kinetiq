package com.kinetiq.plan.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import java.util.List;

/**
 * What a prompt and its checks need. The names are used only to clean text and are never placed in a prompt; the
 * trainer's id tells a result to drop when the client's trainer has changed.
 */
public record DraftInput(
        QuizSubmission answers,
        AccountId trainer,
        String clientName,
        String trainerName,
        Language language,
        List<Exercise> library) {

    public DraftInput {
        library = List.copyOf(library);
    }
}
