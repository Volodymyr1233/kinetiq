package com.kinetiq.account.domain.input;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.common.domain.model.Email;
import java.util.Locale;

public interface SignUpUseCase {

    /** Answers the same whether or not the email already has an account. */
    void signUpTrainer(SignUpTrainerCommand command);

    void confirmEmail(String token);

    /** Answers the same whatever the email, but sends only to an unconfirmed account. */
    void sendConfirmationLink(Email email, Locale locale);

    /**
     * Tells who invited the holder of a working invite link, and changes nothing. Refuses every link that does not
     * work with the same {@code InviteLinkInvalidException}.
     */
    InviteDetails checkInvite(String token);

    /**
     * Creates the client, links them to the inviting trainer, stores when they consented and uses the invite. Refuses
     * a link that does not work as {@link #checkInvite} does, and then changes nothing.
     */
    Account joinAsClient(JoinCommand command);
}
