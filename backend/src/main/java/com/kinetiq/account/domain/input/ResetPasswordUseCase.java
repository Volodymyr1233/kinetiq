package com.kinetiq.account.domain.input;

import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.common.domain.model.Email;
import java.util.Locale;

public interface ResetPasswordUseCase {

    /** Answers the same whatever the email, but sends only to a confirmed account. */
    void requestReset(Email email, Locale locale);

    /** Sets the new password, uses up the link and signs the account out everywhere. */
    void setPassword(String token, RawPassword password);
}
