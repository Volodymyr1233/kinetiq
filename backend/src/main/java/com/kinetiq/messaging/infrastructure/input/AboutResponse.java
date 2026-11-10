package com.kinetiq.messaging.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The logged set a message is about, as it is now: what its chip shows, and the session and place its link leads to.
 * The exercise is named in the language of the request.
 */
record AboutResponse(
        @NotNull UUID setLogId,
        @NotNull UUID sessionLogId,
        @NotNull String sessionName,
        @NotNull LocalDate sessionDate,
        @NotNull String exerciseName,
        @NotNull int entryPosition,
        @NotNull int setNumber,
        @NotNull int rpe) {}
