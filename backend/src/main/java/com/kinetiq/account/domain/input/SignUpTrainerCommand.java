package com.kinetiq.account.domain.input;

import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.common.domain.model.Email;
import java.time.ZoneId;
import java.util.Locale;

/** Signs up a trainer. The locale is the UI language, used for the email. */
public record SignUpTrainerCommand(String name, Email email, RawPassword password, ZoneId timeZone, Locale locale) {}
