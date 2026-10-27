package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.RawPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record AcceptInviteRequest(
        @NotBlank String token,
        @NotBlank @Size(max = Account.NAME_MAX_LENGTH) @Pattern(regexp = "^\\P{Cc}*$") String name,
        @NotNull @Size(min = RawPassword.MIN_LENGTH, max = RawPassword.MAX_LENGTH)
                @Pattern(regexp = RawPassword.CHARACTER_RULES)
                String password,
        @AssertTrue boolean atLeastSixteen,
        @AssertTrue boolean healthDataConsent,
        @NotBlank String timeZone) {}
