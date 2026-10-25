package com.kinetiq.account.domain.model;

import com.kinetiq.account.domain.exception.ConfirmationLinkInvalidException;
import com.kinetiq.account.domain.exception.NameNotChangeableException;
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
    private String name;
    private final Email email;
    private final String passwordHash;
    private final Role role;
    private ZoneId timeZone;
    private AccountSettings settings;
    private boolean emailConfirmed;

    @Getter(AccessLevel.NONE)
    private EmailConfirmation confirmation;

    @Getter(AccessLevel.NONE)
    private Instant mailSentAt;

    public static Account signUpTrainer(
            String name, Email email, String passwordHash, ZoneId timeZone, Language language) {
        return new Account(
                AccountId.random(),
                validName(name),
                Objects.requireNonNull(email),
                Objects.requireNonNull(passwordHash),
                Role.TRAINER,
                Objects.requireNonNull(timeZone),
                AccountSettings.startingWith(Objects.requireNonNull(language)),
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
            AccountSettings settings,
            boolean emailConfirmed,
            EmailConfirmation confirmation,
            Instant mailSentAt) {
        return new Account(
                id, name, email, passwordHash, role, timeZone, settings, emailConfirmed, confirmation, mailSentAt);
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

    /** Follows the browser, so weeks and days stay right after the person moves or travels. */
    public void changeTimeZone(ZoneId zone) {
        timeZone = Objects.requireNonNull(zone);
    }

    /** Changes a trainer's name under the sign-up rules; a client cannot change their name. */
    public void rename(String name) {
        if (role != Role.TRAINER) {
            throw new NameNotChangeableException();
        }
        this.name = validName(name);
    }

    /** Replaces the person's units, language and theme. */
    public void changeSettings(AccountSettings settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    public void recordMailSent(Instant now) {
        mailSentAt = now;
    }

    /** Confirms the email and removes the link, so it confirms at most once. */
    public void confirmEmail(Instant now) {
        if (confirmation == null || !now.isBefore(confirmation.expiresAt())) {
            throw new ConfirmationLinkInvalidException();
        }
        emailConfirmed = true;
        confirmation = null;
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
