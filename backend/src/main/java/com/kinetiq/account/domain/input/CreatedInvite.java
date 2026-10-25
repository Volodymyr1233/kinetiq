package com.kinetiq.account.domain.input;

import com.kinetiq.account.domain.model.Invite;

/** A new invite and the link the trainer may copy; the emailed link is never returned. */
public record CreatedInvite(Invite invite, String link) {}
