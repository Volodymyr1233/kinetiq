package com.kinetiq.quiz.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import java.math.BigDecimal;
import java.util.Set;

/** The answers of {@code client}; {@code notes} may be {@code null}. */
public record SubmitQuizCommand(
        AccountId client,
        Goal goal,
        Experience experience,
        Level level,
        int age,
        int heightCm,
        BigDecimal weightKg,
        Set<Like> likes,
        int daysPerWeek,
        String notes) {}
