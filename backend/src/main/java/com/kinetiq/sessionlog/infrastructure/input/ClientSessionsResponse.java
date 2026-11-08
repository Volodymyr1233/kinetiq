package com.kinetiq.sessionlog.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.util.List;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** Every session of one client as their trainer reads them, newest first. */
record ClientSessionsResponse(@NotNull List<ClientOverviewResponse.ClientSessionResponse> sessions) {}
