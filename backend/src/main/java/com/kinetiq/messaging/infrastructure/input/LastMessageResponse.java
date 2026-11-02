package com.kinetiq.messaging.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

record LastMessageResponse(@NotNull String preview, @NotNull boolean sentByMe, @NotNull Instant sentAt) {}
