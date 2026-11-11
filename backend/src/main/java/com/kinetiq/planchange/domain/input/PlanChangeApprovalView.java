package com.kinetiq.planchange.domain.input;

import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeText;
import java.util.Objects;

/** A plan change as a row of the trainer's lists: with its client's name and its title and reason in the trainer's language. */
public record PlanChangeApprovalView(PlanChange planChange, String clientName, PlanChangeText text) {

    public PlanChangeApprovalView {
        Objects.requireNonNull(planChange);
        Objects.requireNonNull(clientName);
        Objects.requireNonNull(text);
    }
}
