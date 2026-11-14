package com.kinetiq.notification.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.common.domain.model.NoticeKind;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * One notification. {@code clientId} and {@code clientName} are left out unless it is about a client who still exists.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record NotificationResponse(
        @NotNull UUID id,
        @NotNull NoticeKind kind,
        @NotNull Instant createdAt,
        @NotNull boolean read,
        UUID clientId,
        String clientName) {}
