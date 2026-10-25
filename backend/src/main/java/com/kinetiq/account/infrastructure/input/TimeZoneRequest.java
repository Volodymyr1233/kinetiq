package com.kinetiq.account.infrastructure.input;

import jakarta.validation.constraints.NotBlank;

record TimeZoneRequest(@NotBlank String timeZone) {}
