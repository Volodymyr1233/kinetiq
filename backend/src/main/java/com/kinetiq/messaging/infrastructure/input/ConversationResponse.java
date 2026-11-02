package com.kinetiq.messaging.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.quiz.domain.model.Goal;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * One conversation in a person's list: {@code name} is the other person's. {@code goal} is left out before the quiz,
 * the two week fields until the plan is published, and {@code lastMessage} while nobody has written.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ConversationResponse(
        @NotNull UUID clientId,
        @NotNull String name,
        Goal goal,
        Integer currentWeek,
        Integer planWeeks,
        @NotNull int unreadCount,
        LastMessageResponse lastMessage) {}
