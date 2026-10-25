package com.kinetiq.account.domain.model;

import java.util.Locale;

/** The language the app is shown in. */
public enum Language {
    EN,
    PL;

    /** Polish gives {@code PL}; any other locale gives {@code EN}, the app's fallback language. */
    public static Language from(Locale locale) {
        return "pl".equals(locale.getLanguage()) ? PL : EN;
    }
}
