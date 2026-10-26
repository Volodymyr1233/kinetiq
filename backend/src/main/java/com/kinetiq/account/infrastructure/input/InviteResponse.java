package com.kinetiq.account.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** An open invite as the trainer sees it; never its secrets. */
record InviteResponse(@NotNull UUID id, @NotNull String email, @NotNull Instant sentAt, @NotNull Instant expiresAt) {}
