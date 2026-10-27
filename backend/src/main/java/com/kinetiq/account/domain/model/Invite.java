package com.kinetiq.account.domain.model;

import com.kinetiq.account.domain.exception.InviteNotFoundException;
import com.kinetiq.common.domain.model.Email;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** A trainer's single-use offer to one email to join as their client. */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Invite {

    public static final Duration VALIDITY = Duration.ofDays(7);

    private final InviteId id;
    private final AccountId trainerId;
    private final Email email;
    private InviteStatus status;
    private final String emailTokenHash;
    private String linkTokenHash;
    private final Instant sentAt;
    private final Instant expiresAt;

    /** Creates an open invite with one secret for the email and another for the trainer to copy. */
    public static SentInvite send(AccountId trainerId, Email email, Instant now) {
        SecretToken emailToken = SecretToken.generate();
        SecretToken linkToken = SecretToken.generate();
        Invite invite = new Invite(
                InviteId.random(),
                Objects.requireNonNull(trainerId),
                Objects.requireNonNull(email),
                InviteStatus.OPEN,
                emailToken.hash(),
                linkToken.hash(),
                now,
                now.plus(VALIDITY));
        return new SentInvite(invite, emailToken, linkToken);
    }

    public static Invite restore(
            InviteId id,
            AccountId trainerId,
            Email email,
            InviteStatus status,
            String emailTokenHash,
            String linkTokenHash,
            Instant sentAt,
            Instant expiresAt) {
        return new Invite(id, trainerId, email, status, emailTokenHash, linkTokenHash, sentAt, expiresAt);
    }

    public boolean isOpen(Instant now) {
        return status == InviteStatus.OPEN && now.isBefore(expiresAt);
    }

    /** Whether the token is the emailed secret, the one that proves control of the address. */
    public boolean provesEmail(SecretToken token) {
        return token.hash().equals(emailTokenHash);
    }

    /** Issues a new secret for the trainer to copy; the one copied before stops working, the emailed one does not. */
    public SecretToken replaceLink(Instant now) {
        requireOpen(now);
        SecretToken token = SecretToken.generate();
        linkTokenHash = token.hash();
        return token;
    }

    public void revoke(Instant now) {
        requireOpen(now);
        status = InviteStatus.REVOKED;
    }

    public void use(Instant now) {
        requireOpen(now);
        status = InviteStatus.USED;
    }

    /** Ends an invite replaced by a newer one for the same email, even after it expired. */
    public void supersede() {
        if (status == InviteStatus.OPEN) {
            status = InviteStatus.REVOKED;
        }
    }

    private void requireOpen(Instant now) {
        if (!isOpen(now)) {
            throw new InviteNotFoundException();
        }
    }
}
