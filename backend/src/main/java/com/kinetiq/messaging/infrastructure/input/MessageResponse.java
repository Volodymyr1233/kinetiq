package com.kinetiq.messaging.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * A message from the reader's side: {@code mine} says whether they wrote it, and nothing tells who has read it.
 * {@code about} is left out when the message is about no set.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record MessageResponse(
        @NotNull UUID id, @NotNull String text, @NotNull boolean mine, @NotNull Instant sentAt, AboutResponse about) {}
