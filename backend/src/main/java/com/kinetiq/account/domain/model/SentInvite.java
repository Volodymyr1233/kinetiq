package com.kinetiq.account.domain.model;

/** A new invite with its two secrets, which exist in readable form only until they are sent. */
public record SentInvite(Invite invite, SecretToken emailToken, SecretToken linkToken) {}
