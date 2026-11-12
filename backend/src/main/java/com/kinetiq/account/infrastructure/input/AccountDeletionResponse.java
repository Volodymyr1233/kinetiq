package com.kinetiq.account.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

/** The trainer's number of clients (0 for a client), and a client's trainer's name, left out for a trainer. */
@JsonInclude(JsonInclude.Include.NON_NULL)
record AccountDeletionResponse(@NotNull int clients, String trainerName) {}
