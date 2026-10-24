package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.model.Role;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** Who is signed in. Settings join with task #43. */
record CurrentAccountResponse(@NotNull UUID id, @NotNull String name, @NotNull Role role, @NotNull String timeZone) {}
