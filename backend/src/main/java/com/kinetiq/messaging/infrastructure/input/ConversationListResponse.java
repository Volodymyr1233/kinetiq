package com.kinetiq.messaging.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.util.List;

record ConversationListResponse(@NotNull List<ConversationResponse> conversations) {}
