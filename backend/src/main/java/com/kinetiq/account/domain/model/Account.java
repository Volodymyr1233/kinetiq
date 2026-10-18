package com.kinetiq.account.domain.model;

import com.kinetiq.common.domain.exception.InvalidValueException;
import com.kinetiq.common.domain.model.Email;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Account {

    public static final int NAME_MAX_LENGTH = 100;

    private static final Pattern CONTROL_CHARACTER = Pattern.compile("\\p{Cc}");
    private static final Duration CONFIRMATION_VALIDITY = Duration.ofHours(24);
    private static final Duration MAIL_INTERVAL = Duration.ofSeconds(60);

    private final AccountId id;
    private final String name;
    private final Email email;
    private final String passwordHash;
    private final Role role;
    private final ZoneId timeZone;
    private boolean emailConfirmed;

    @Getter(AccessLevel.NONE)
    private EmailConfirmation confirmation;

    @Getter(AccessLevel.NONE)
    private Instant mailSentAt;

    public static Account signUpTrainer(String name, Email email, String passwordHash, ZoneId timeZone) {
        return new Account(
                AccountId.random(),
                validName(name),
                Objects.requireNonNull(email),
                Objects.requireNonNull(passwordHash),
                Role.TRAINER,
                Objects.requireNonNull(timeZone),
                false,
                null,
                null);
    }

    public static Account restore(
            AccountId id,
            String name,
            Email email,
            String passwordHash,
            Role role,
            ZoneId timeZone,
            boolean emailConfirmed,
            EmailConfirmation confirmation,
            Instant mailSentAt) {
        return new Account(
                id, name, email, passwordHash, role, timeZone, emailConfirmed, confirmation, mailSentAt);
    }

    public Optional<EmailConfirmation> getConfirmation() {
        return Optional.ofNullable(confirmation);
    }

    public Optional<Instant> getMailSentAt() {
        return Optional.ofNullable(mailSentAt);
    }

    /** Allows at most one email per account in any 60 seconds, whatever its kind. */
    public boolean canReceiveMail(Instant now) {
        return mailSentAt == null || !now.isBefore(mailSentAt.plus(MAIL_INTERVAL));
    }

    /** Issues a new confirmation link and replaces any earlier one, so only the newest works. */
    public void issueConfirmation(ConfirmationToken token, Instant now) {
        if (emailConfirmed) {
            throw new IllegalStateException("email is already confirmed");
        }
        confirmation = new EmailConfirmation(token.hash(), now.plus(CONFIRMATION_VALIDITY));
        mailSentAt = now;
    }

    public void recordMailSent(Instant now) {
        mailSentAt = now;
    }

    private static String validName(String name) {
        String trimmed = Objects.requireNonNullElse(name, "").strip();
        if (trimmed.isEmpty()
                || trimmed.length() > NAME_MAX_LENGTH
                || CONTROL_CHARACTER.matcher(trimmed).find()) {
            throw new InvalidValueException("name is blank, too long or has control characters");
        }
        return trimmed;
    }
}
