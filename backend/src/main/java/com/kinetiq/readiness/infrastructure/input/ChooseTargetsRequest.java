package com.kinetiq.readiness.infrastructure.input;

import com.kinetiq.readiness.domain.model.TargetChoice;
import jakarta.validation.constraints.NotNull;

/** Which targets the client trains with today. */
record ChooseTargetsRequest(@NotNull TargetChoice choice) {}
