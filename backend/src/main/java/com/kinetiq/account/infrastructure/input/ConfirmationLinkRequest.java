package com.kinetiq.account.infrastructure.input;

import com.kinetiq.common.domain.model.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record ConfirmationLinkRequest(
        @NotBlank @Size(max = Email.MAX_LENGTH) @Pattern(regexp = SignUpRequest.EMAIL) String email) {}
