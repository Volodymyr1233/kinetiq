package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.model.RawPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record SetPasswordRequest(
        @NotBlank String token,
        @NotNull @Size(min = RawPassword.MIN_LENGTH, max = RawPassword.MAX_LENGTH)
                @Pattern(regexp = RawPassword.CHARACTER_RULES)
                String password) {}
