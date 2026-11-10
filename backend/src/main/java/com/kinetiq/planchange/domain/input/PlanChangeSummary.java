package com.kinetiq.planchange.domain.input;

import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeText;
import java.util.Objects;

/** The open plan change of a client, as the client page shows it. */
public record PlanChangeSummary(PlanChange planChange, PlanChangeText text) {

    public PlanChangeSummary {
        Objects.requireNonNull(planChange);
        Objects.requireNonNull(text);
    }
}
