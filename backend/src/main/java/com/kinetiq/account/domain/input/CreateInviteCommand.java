package com.kinetiq.account.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Email;
import java.util.Locale;

/** Invites an email for a trainer. The locale is the trainer's UI language, used for the email. */
public record CreateInviteCommand(AccountId trainerId, Email email, Locale locale) {}
