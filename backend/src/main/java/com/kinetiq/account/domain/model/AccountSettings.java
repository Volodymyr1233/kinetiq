package com.kinetiq.account.domain.model;

import java.util.Objects;

/** A person's units, language and theme, kept with the account so they follow the person to every device. */
public record AccountSettings(WeightUnit units, Language language, Theme theme) {

    public AccountSettings {
        Objects.requireNonNull(units);
        Objects.requireNonNull(language);
        Objects.requireNonNull(theme);
    }

    /** The settings a new account starts with: kilograms, the given language and the device's theme. */
    public static AccountSettings startingWith(Language language) {
        return new AccountSettings(WeightUnit.KG, language, Theme.SYSTEM);
    }

    /** Returns these settings with each non-null argument replacing its value; a null argument keeps the value. */
    public AccountSettings with(WeightUnit units, Language language, Theme theme) {
        return new AccountSettings(
                Objects.requireNonNullElse(units, this.units),
                Objects.requireNonNullElse(language, this.language),
                Objects.requireNonNullElse(theme, this.theme));
    }
}
