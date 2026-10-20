package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.common.domain.model.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record SignUpRequest(
        @NotBlank @Size(max = Account.NAME_MAX_LENGTH) @Pattern(regexp = "^\\P{Cc}*$") String name,
        @NotBlank @Size(max = Email.MAX_LENGTH) @Pattern(regexp = EMAIL) String email,
        @NotNull @Size(min = RawPassword.MIN_LENGTH, max = RawPassword.MAX_LENGTH)
                @Pattern(regexp = RawPassword.CHARACTER_RULES)
                String password,
        @AssertTrue boolean adult,
        @NotBlank String timeZone) {

    // surrounding spaces are allowed here because Email trims them
    static final String EMAIL = "^\\s*[^\\s@]+@[^\\s@]+\\.[^\\s@]+\\s*$";
}
