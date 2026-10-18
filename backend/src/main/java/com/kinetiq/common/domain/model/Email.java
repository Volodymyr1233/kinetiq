package com.kinetiq.common.domain.model;

import com.kinetiq.common.domain.exception.InvalidValueException;
import java.util.Locale;
import java.util.regex.Pattern;

/** An email address, normalised once on the way in so equal addresses compare equal in code and in the database. */
public record Email(String value) {

    public static final int MAX_LENGTH = 254;

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Email {
        if (value == null) {
            throw new InvalidValueException("email is missing");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new InvalidValueException("email is malformed");
        }
    }
}
