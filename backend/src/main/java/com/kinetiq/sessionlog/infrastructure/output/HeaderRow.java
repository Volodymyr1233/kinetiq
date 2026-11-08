package com.kinetiq.sessionlog.infrastructure.output;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** The columns the state of a planned session needs, read without the entries and sets. */
record HeaderRow(UUID id, LocalDate logDate, Instant startedAt, Instant finishedAt) {}
