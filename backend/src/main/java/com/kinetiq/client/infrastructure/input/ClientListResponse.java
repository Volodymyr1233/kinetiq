package com.kinetiq.client.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.util.List;

record ClientListResponse(@NotNull List<ClientResponse> clients) {}
