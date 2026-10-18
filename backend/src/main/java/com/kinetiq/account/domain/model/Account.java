package com.kinetiq.account.domain.model;

import com.kinetiq.common.domain.exception.InvalidValueException;
import com.kinetiq.common.domain.model.Email;
import java.time.ZoneId;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Account {

    public static final int NAME_MAX_LENGTH = 100;

    private static final Pattern CONTROL_CHARACTER = Pattern.compile("\\p{Cc}");

    private final AccountId id;
    private final String name;
    private final Email email;
    private final String passwordHash;
    private final Role role;
    private final ZoneId timeZone;
    private boolean emailConfirmed;

    public static Account signUpTrainer(String name, Email email, String passwordHash, ZoneId timeZone) {
        return new Account(
                AccountId.random(),
                validName(name),
                Objects.requireNonNull(email),
                Objects.requireNonNull(passwordHash),
                Role.TRAINER,
                Objects.requireNonNull(timeZone),
                false);
    }

    public static Account restore(
            AccountId id,
            String name,
            Email email,
            String passwordHash,
            Role role,
            ZoneId timeZone,
            boolean emailConfirmed) {
        return new Account(id, name, email, passwordHash, role, timeZone, emailConfirmed);
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
