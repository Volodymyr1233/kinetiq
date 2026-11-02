package com.kinetiq.messaging.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** At most 50 messages, oldest to newest; {@code hasMore} says whether earlier ones exist. */
record MessagePageResponse(@NotNull List<MessageResponse> messages, @NotNull boolean hasMore) {}
