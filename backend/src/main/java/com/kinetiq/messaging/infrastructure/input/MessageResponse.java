package com.kinetiq.messaging.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** A message from the reader's side: {@code mine} says whether they wrote it, and nothing tells who has read it. */
record MessageResponse(@NotNull UUID id, @NotNull String text, @NotNull boolean mine, @NotNull Instant sentAt) {}
