package com.kinetiq.account.infrastructure.input;

import jakarta.validation.constraints.NotBlank;

record ConfirmEmailRequest(@NotBlank String token) {}
