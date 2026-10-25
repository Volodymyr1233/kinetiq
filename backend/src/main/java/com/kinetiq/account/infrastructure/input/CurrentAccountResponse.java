package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.model.Theme;
import com.kinetiq.account.domain.model.WeightUnit;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** Who is signed in, with their settings. */
record CurrentAccountResponse(
        @NotNull UUID id,
        @NotNull String name,
        @NotNull String email,
        @NotNull Role role,
        @NotNull String timeZone,
        @NotNull WeightUnit units,
        @NotNull Language language,
        @NotNull Theme theme) {}
