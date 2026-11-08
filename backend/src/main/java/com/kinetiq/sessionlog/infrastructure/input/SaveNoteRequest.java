package com.kinetiq.sessionlog.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// the limit of 500 is checked by the domain after trimming; this only keeps a huge body out
/** The note for the trainer; empty text removes it. */
record SaveNoteRequest(@NotNull @Size(max = 2000) String text) {}
