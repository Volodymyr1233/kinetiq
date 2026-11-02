package com.kinetiq.messaging.domain.model;

import com.kinetiq.messaging.domain.exception.MessageEmptyException;
import com.kinetiq.messaging.domain.exception.MessageTooLongException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The text of a message: 1 to 2000 UTF-16 units, as the browser's {@code maxLength} counts, after removing spaces and
 * line breaks around it. Line breaks inside it are kept.
 */
public record MessageText(String value) {

    public static final int MAX_LENGTH = 2000;

    // ponytail: the quiz notes' pattern, a second use; move it to common at a third
    private static final Pattern EDGE_SPACES = Pattern.compile("^[\\s\\p{Z}]+|[\\s\\p{Z}]+$");

    public MessageText {
        // PostgreSQL cannot store a NUL, and it means nothing in a message
        value = EDGE_SPACES.matcher(Objects.requireNonNull(value).replace("\0", "")).replaceAll("");
        if (value.isEmpty()) {
            throw new MessageEmptyException();
        }
        if (value.length() > MAX_LENGTH) {
            throw new MessageTooLongException();
        }
    }

    // a record would print the text, which can hold health details and must never reach a log
    @Override
    public String toString() {
        return "MessageText[" + value.length() + " units]";
    }
}
