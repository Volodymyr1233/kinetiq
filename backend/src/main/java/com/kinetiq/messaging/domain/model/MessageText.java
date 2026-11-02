package com.kinetiq.messaging.domain.model;

import com.kinetiq.messaging.domain.exception.MessageEmptyException;
import com.kinetiq.messaging.domain.exception.MessageTooLongException;
import java.util.Objects;

/**
 * The text of a message: 1 to 2000 UTF-16 units, as the browser's {@code maxLength} counts, after removing spaces and
 * line breaks around it. Line breaks inside it are kept.
 */
public record MessageText(String value) {

    public static final int MAX_LENGTH = 2000;

    public MessageText {
        // PostgreSQL cannot store a NUL, and it means nothing in a message
        value = trimmed(Objects.requireNonNull(value).replace("\0", ""));
        if (value.isEmpty()) {
            throw new MessageEmptyException();
        }
        if (value.length() > MAX_LENGTH) {
            throw new MessageTooLongException();
        }
    }

    // a loop and not the quiz notes' pattern: this runs before the length check, and that regex is quadratic on a
    // long run of spaces inside the text; it removes the same characters, \\s and \\p{Z}
    private static String trimmed(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && isEdgeSpace(text.charAt(start))) {
            start++;
        }
        while (end > start && isEdgeSpace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(start, end);
    }

    private static boolean isEdgeSpace(char character) {
        return Character.isSpaceChar(character) || "\t\n\u000B\f\r".indexOf(character) >= 0;
    }

    // a record would print the text, which can hold health details and must never reach a log
    @Override
    public String toString() {
        return "MessageText[" + value.length() + " units]";
    }
}
