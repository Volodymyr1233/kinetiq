package com.kinetiq.sessionlog.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * A set to log. {@code loadKg} is left out for no added weight. The limits of the values are the domain's, so a value
 * outside them is refused with its field.
 */
record LogSetRequest(
        @NotNull Integer entryPosition,
        @NotNull Integer setNumber,
        BigDecimal loadKg,
        @NotNull Integer amount,
        @NotNull Integer rpe) {}
