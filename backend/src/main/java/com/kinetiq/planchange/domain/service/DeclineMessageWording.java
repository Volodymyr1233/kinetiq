package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.Language;
import com.kinetiq.planchange.domain.model.DeclineReason;

/**
 * The message a person's decision to keep the current plan leaves in the conversation: plain text in the language of
 * the person who decided. The first line names the plan change and the reason, the note, when there is one, follows
 * on a line of its own. It uses no pronoun, so it fits any sender.
 */
public final class DeclineMessageWording {

    private DeclineMessageWording() {}

    /** {@code note} is the trimmed note of the decision, or {@code null}. */
    public static String of(Language language, String title, DeclineReason reason, String note) {
        boolean polish = language == Language.PL;
        String first = polish
                ? "Zostaję przy obecnym planie zamiast: %s. Powód: %s.".formatted(title, label(true, reason))
                : "Keeping the current plan instead of: %s. Reason: %s.".formatted(title, label(false, reason));
        return note == null ? first : first + "\n" + note;
    }

    /** The reason as a short label, for the same people and the same language as the message. */
    static String label(boolean polish, DeclineReason reason) {
        return switch (reason) {
            case TOO_EASY -> polish ? "za łatwy" : "too easy";
            case TOO_HARD -> polish ? "za trudny" : "too hard";
            case SCHEDULE -> polish ? "harmonogram" : "schedule";
            case INJURY -> polish ? "kontuzja" : "injury";
            case OTHER -> polish ? "inne" : "other";
        };
    }
}
