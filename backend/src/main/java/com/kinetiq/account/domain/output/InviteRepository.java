package com.kinetiq.account.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.InviteId;
import com.kinetiq.common.domain.model.Email;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InviteRepository {

    void save(Invite invite);

    Optional<Invite> findById(InviteId id);

    /** Returns the trainer's open invites that have not expired at {@code now}, newest first. */
    List<Invite> findOpenByTrainer(AccountId trainerId, Instant now);

    /** Returns the trainer's open invite for the email, whether or not it has expired. */
    Optional<Invite> findOpenByTrainerAndEmail(AccountId trainerId, Email email);

    /** Returns the invite whose email secret or link secret has this hash. */
    Optional<Invite> findByTokenHash(String tokenHash);
}
