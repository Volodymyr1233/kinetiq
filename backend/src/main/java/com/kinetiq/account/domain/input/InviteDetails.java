package com.kinetiq.account.domain.input;

import com.kinetiq.common.domain.model.Email;

/** What a working invite link shows the person who opened it: the invited email and who invited them. */
public record InviteDetails(Email email, String trainerName) {}
