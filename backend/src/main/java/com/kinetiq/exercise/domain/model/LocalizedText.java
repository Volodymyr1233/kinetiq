package com.kinetiq.exercise.domain.model;

import java.util.Locale;
import java.util.Objects;

/** A text in English, optionally also in Polish; {@code pl} is empty when there is no Polish text. */
public record LocalizedText(String en, String pl) {

    public LocalizedText {
        Objects.requireNonNull(en);
        pl = Objects.requireNonNullElse(pl, "");
    }

    /** Returns the Polish text for a Polish locale when there is one, else the English text. */
    public String in(Locale locale) {
        return locale.getLanguage().equals("pl") && !pl.isBlank() ? pl : en;
    }
}
