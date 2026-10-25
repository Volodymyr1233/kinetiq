package com.kinetiq.account.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.util.List;

record InviteListResponse(@NotNull List<InviteResponse> invites) {}
