package com.kinetiq.plan.infrastructure.input;

import com.kinetiq.plan.domain.model.DraftedPlan;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** The trainer's drafted plans, the one waiting longest first. */
record DraftedPlansResponse(@NotNull List<DraftedPlanResponse> items) {

    /** One client's draft; {@code createdAt} is when the plan was first stored. */
    record DraftedPlanResponse(
            @NotNull UUID clientId,
            @NotNull String clientName,
            @NotNull DraftedPlan.Kind kind,
            @NotNull Instant createdAt,
            @NotNull int weeks,
            @NotNull int sessionsPerWeek) {}

    static DraftedPlansResponse of(List<DraftedPlan> drafts) {
        return new DraftedPlansResponse(drafts.stream()
                .map(draft -> new DraftedPlanResponse(
                        draft.client().value(),
                        draft.clientName(),
                        draft.kind(),
                        draft.createdAt(),
                        draft.weeks(),
                        draft.sessionsPerWeek()))
                .toList());
    }
}
