package com.kinetiq.account.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.InviteId;
import java.util.List;

public interface ManageInvitesUseCase {

    /**
     * Invites the email and sends it a link; returns another link for the trainer to copy. An open invite of the same
     * trainer for the email is revoked first. Refused when the email already has an account.
     */
    CreatedInvite create(CreateInviteCommand command);

    /** Lists the trainer's open invites, newest first. */
    List<Invite> listOpen(AccountId trainerId);

    /**
     * Replaces the link the trainer copies and returns the new one. An unknown invite, another trainer's, and one that
     * is no longer open are all refused as not found.
     */
    String replaceLink(AccountId trainerId, InviteId id);

    /** Revokes the invite, so neither of its links works. Refused as not found like {@link #replaceLink}. */
    void revoke(AccountId trainerId, InviteId id);
}
