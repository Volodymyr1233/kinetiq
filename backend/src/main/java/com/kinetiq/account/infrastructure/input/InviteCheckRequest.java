package com.kinetiq.account.infrastructure.input;

import jakarta.validation.constraints.NotBlank;

record InviteCheckRequest(@NotBlank String token) {}
