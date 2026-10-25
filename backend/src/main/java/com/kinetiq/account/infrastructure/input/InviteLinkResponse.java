package com.kinetiq.account.infrastructure.input;

import jakarta.validation.constraints.NotNull;

record InviteLinkResponse(@NotNull String link) {}
