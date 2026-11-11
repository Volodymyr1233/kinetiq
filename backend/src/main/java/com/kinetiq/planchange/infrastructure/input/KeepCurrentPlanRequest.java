package com.kinetiq.planchange.infrastructure.input;

import com.kinetiq.planchange.domain.model.DeclineReason;
import com.kinetiq.planchange.domain.model.Decision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Keeps the current plan instead of accepting a plan change; the note is plain text and may be left out. */
record KeepCurrentPlanRequest(@NotNull DeclineReason reason, @Size(max = Decision.NOTE_MAX_LENGTH) String note) {}
