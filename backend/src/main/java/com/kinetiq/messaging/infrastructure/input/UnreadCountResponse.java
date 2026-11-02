package com.kinetiq.messaging.infrastructure.input;

import jakarta.validation.constraints.NotNull;

record UnreadCountResponse(@NotNull int count) {}
