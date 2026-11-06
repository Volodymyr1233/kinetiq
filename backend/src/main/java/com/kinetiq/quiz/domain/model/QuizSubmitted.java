package com.kinetiq.quiz.domain.model;

import com.kinetiq.account.domain.model.AccountId;

/** A client's answers have just been stored. */
public record QuizSubmitted(AccountId client) {}
