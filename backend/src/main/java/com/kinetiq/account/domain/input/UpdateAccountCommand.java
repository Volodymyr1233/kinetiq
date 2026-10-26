package com.kinetiq.account.domain.input;

import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Theme;
import com.kinetiq.account.domain.model.WeightUnit;

/**
 * Changes the signed-in person's name and settings. A null value keeps what is stored, so only the values sent
 * change.
 */
public record UpdateAccountCommand(String name, WeightUnit units, Language language, Theme theme) {}
