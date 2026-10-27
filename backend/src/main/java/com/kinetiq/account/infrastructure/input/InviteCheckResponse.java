package com.kinetiq.account.infrastructure.input;

import jakarta.validation.constraints.NotNull;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** Who sent a working invite, and to which email. */
record InviteCheckResponse(@NotNull String email, @NotNull String trainerName) {}
