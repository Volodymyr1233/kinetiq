package com.kinetiq.account.domain.model;

import com.kinetiq.account.domain.exception.ConfirmationLinkInvalidException;
import com.kinetiq.account.domain.exception.NameNotChangeableException;
import com.kinetiq.account.domain.exception.PasswordResetLinkInvalidException;
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
    private static final Duration RESET_VALIDITY = Duration.ofHours(1);
    private static final Duration MAIL_INTERVAL = Duration.ofSeconds(60);

    private final AccountId id;
    private String name;
    private final Email email;
    private String passwordHash;
    private final Role role;

    @Getter(AccessLevel.NONE)
    private final AccountId trainerId;

    private ZoneId timeZone;
    private AccountSettings settings;
    private boolean emailConfirmed;

    @Getter(AccessLevel.NONE)
    private EmailConfirmation confirmation;

    @Getter(AccessLevel.NONE)
    private Instant mailSentAt;

    @Getter(AccessLevel.NONE)
    private PasswordReset passwordReset;

    @Getter(AccessLevel.NONE)
    private final Instant healthDataConsentAt;

    public static Account signUpTrainer(
            String name, Email email, String passwordHash, ZoneId timeZone, Language language) {
        return new Account(
                AccountId.random(),
                validName(name),
                Objects.requireNonNull(email),
                Objects.requireNonNull(passwordHash),
                Role.TRAINER,
                null,
                Objects.requireNonNull(timeZone),
                AccountSettings.startingWith(Objects.requireNonNull(language)),
                false,
                null,
                null,
                null,
                null);
    }

    /**
     * Creates a client who joined through their trainer's invite. The email is confirmed only when the person proved
     * they control it, by using the emailed secret. Every argument is required.
     */
    public static Account joinAsClient(
            String name,
            Email email,
            String passwordHash,
            ZoneId timeZone,
            AccountSettings settings,
            AccountId trainerId,
            Instant consentAt,
            boolean emailConfirmed) {
        return new Account(
                AccountId.random(),
                validName(name),
                Objects.requireNonNull(email),
                Objects.requireNonNull(passwordHash),
                Role.CLIENT,
                Objects.requireNonNull(trainerId),
                Objects.requireNonNull(timeZone),
                Objects.requireNonNull(settings),
                emailConfirmed,
                null,
                null,
                null,
                Objects.requireNonNull(consentAt));
    }

    public static Account restore(
            AccountId id,
            String name,
            Email email,
            String passwordHash,
            Role role,
            AccountId trainerId,
            ZoneId timeZone,
            AccountSettings settings,
            boolean emailConfirmed,
            EmailConfirmation confirmation,
            Instant mailSentAt,
            PasswordReset passwordReset,
            Instant healthDataConsentAt) {
        return new Account(
                id,
                name,
                email,
                passwordHash,
                role,
                trainerId,
                timeZone,
                settings,
                emailConfirmed,
                confirmation,
                mailSentAt,
                passwordReset,
                healthDataConsentAt);
    }

    /** The client's trainer; empty for a trainer. */
    public Optional<AccountId> getTrainerId() {
        return Optional.ofNullable(trainerId);
    }

    /** When a client agreed to the processing of their health data; empty for a trainer. */
    public Optional<Instant> getHealthDataConsentAt() {
        return Optional.ofNullable(healthDataConsentAt);
    }

    public Optional<EmailConfirmation> getConfirmation() {
        return Optional.ofNullable(confirmation);
    }

    public Optional<PasswordReset> getPasswordReset() {
        return Optional.ofNullable(passwordReset);
    }

    public Optional<Instant> getMailSentAt() {
        return Optional.ofNullable(mailSentAt);
    }

    /** Allows at most one email per account in any 60 seconds, whatever its kind. */
    public boolean canReceiveMail(Instant now) {
        return mailSentAt == null || !now.isBefore(mailSentAt.plus(MAIL_INTERVAL));
    }

    /** Issues a new confirmation link and replaces any earlier one, so only the newest works. */
    public void issueConfirmation(SecretToken token, Instant now) {
        if (emailConfirmed) {
            throw new IllegalStateException("email is already confirmed");
        }
        confirmation = new EmailConfirmation(token.hash(), now.plus(CONFIRMATION_VALIDITY));
        mailSentAt = now;
    }

    /** Issues a new password reset link and replaces any earlier one, so only the newest works. */
    public void issuePasswordReset(SecretToken token, Instant now) {
        passwordReset = new PasswordReset(token.hash(), now.plus(RESET_VALIDITY));
        mailSentAt = now;
    }

    /** Replaces the password and removes the link, so it works at most once. */
    public void resetPassword(String newPasswordHash, Instant now) {
        if (passwordReset == null || !now.isBefore(passwordReset.expiresAt())) {
            throw new PasswordResetLinkInvalidException();
        }
        passwordHash = Objects.requireNonNull(newPasswordHash);
        passwordReset = null;
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
