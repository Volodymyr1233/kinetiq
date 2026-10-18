package com.kinetiq.account.domain.model;

import com.kinetiq.common.domain.exception.InvalidValueException;

/** A password as typed. It exists only on its way to the hasher. */
public record RawPassword(String value) {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    /** The character-class rule, for request validation. Length is checked separately. */
    public static final String CHARACTER_RULES = "(?s)^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).*$";

    // length in UTF-16 units, as the browser counts it
    public RawPassword {
        if (value == null
                || value.length() < MIN_LENGTH
                || value.length() > MAX_LENGTH
                || !value.matches(CHARACTER_RULES)) {
            throw new InvalidValueException("password breaks the rules");
        }
    }

    @Override
    public String toString() {
        return "***";
    }
}
