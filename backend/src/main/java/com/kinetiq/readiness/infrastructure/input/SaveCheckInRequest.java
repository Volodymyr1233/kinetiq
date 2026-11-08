package com.kinetiq.readiness.infrastructure.input;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The client's three answers, each a whole number from 1 to 5. */
record SaveCheckInRequest(
        @NotNull @Min(1) @Max(5) Integer sleep,
        @NotNull @Min(1) @Max(5) Integer soreness,
        @NotNull @Min(1) @Max(5) Integer stress) {}
