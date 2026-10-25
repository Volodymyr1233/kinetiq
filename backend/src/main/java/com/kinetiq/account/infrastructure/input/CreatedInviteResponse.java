package com.kinetiq.account.infrastructure.input;

import jakarta.validation.constraints.NotNull;

/** The new invite and the link the trainer may copy. */
record CreatedInviteResponse(@NotNull InviteResponse invite, @NotNull String link) {}
