package com.kinetiq.messaging.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// the rules of the text live in MessageText only, so each has its own code and the limit is written once
/** One attempt to send a message; {@code id} is chosen by the sender and stays the same on a retry. */
record SendMessageRequest(@NotNull UUID id, @NotNull String text) {}
