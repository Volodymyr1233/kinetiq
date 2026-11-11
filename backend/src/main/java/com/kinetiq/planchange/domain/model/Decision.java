package com.kinetiq.planchange.domain.model;

import com.kinetiq.common.domain.exception.InvalidValueException;
import java.util.Objects;

/** How a declined plan change ended: who kept the current plan, why, and an optional plain text note. */
public record Decision(Side by, DeclineReason reason, String note) {

    public static final int NOTE_MAX_LENGTH = 500;

    /** The note is trimmed, and a blank one is none. */
    public Decision {
        Objects.requireNonNull(by);
        Objects.requireNonNull(reason);
        note = note == null || note.isBlank() ? null : note.strip();
        if (note != null && note.length() > NOTE_MAX_LENGTH) {
            throw new InvalidValueException("the note has at most " + NOTE_MAX_LENGTH + " characters");
        }
    }
}
