package com.kinetiq.account.domain.input;

import com.kinetiq.account.domain.model.RawPassword;
import java.time.ZoneId;
import java.util.Locale;

/** Joins as a client through an invite link. The locale is the UI language, the client's starting language. */
public record JoinCommand(String token, String name, RawPassword password, ZoneId timeZone, Locale locale) {}
