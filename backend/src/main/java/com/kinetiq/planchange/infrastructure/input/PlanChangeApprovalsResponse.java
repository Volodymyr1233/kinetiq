package com.kinetiq.planchange.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.util.List;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
record PlanChangeApprovalsResponse(@NotNull List<PlanChangeApprovalResponse> items) {}
