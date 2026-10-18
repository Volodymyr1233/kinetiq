package com.kinetiq.account.domain.input;

public interface SignUpUseCase {

    /** Answers the same whether or not the email already has an account. */
    void signUpTrainer(SignUpTrainerCommand command);
}
