package com.kinetiq.account.domain.input;

import com.kinetiq.common.domain.model.Email;
import java.util.Locale;

public interface SignUpUseCase {

    /** Answers the same whether or not the email already has an account. */
    void signUpTrainer(SignUpTrainerCommand command);

    void confirmEmail(String token);

    /** Answers the same whatever the email, but sends only to an unconfirmed account. */
    void sendConfirmationLink(Email email, Locale locale);
}
