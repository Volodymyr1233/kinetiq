package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.planchange.domain.exception.PlanChangeDrafterUnavailableException;
import com.kinetiq.planchange.domain.model.PlanChangePromptMessages;
import com.kinetiq.planchange.domain.model.ProposedContent;
import com.kinetiq.planchange.domain.output.PlanChangeDrafter;

/** Stands in when no key is configured, so every plan change gets the fixed transformation of its rule. */
public class UnavailablePlanChangeDrafter implements PlanChangeDrafter {

    @Override
    public ProposedContent draft(PlanChangePromptMessages messages) {
        throw new PlanChangeDrafterUnavailableException();
    }
}
