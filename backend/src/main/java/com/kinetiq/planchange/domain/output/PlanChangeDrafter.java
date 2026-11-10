package com.kinetiq.planchange.domain.output;

import com.kinetiq.planchange.domain.model.PlanChangePromptMessages;
import com.kinetiq.planchange.domain.model.ProposedContent;

/** The service that drafts plan change content. One call is one attempt. */
public interface PlanChangeDrafter {

    /**
     * The service's answer, unchecked. Throws {@code PlanChangeDraftingFailedException} when there is no readable
     * answer and {@code PlanChangeDrafterUnavailableException} when no service is configured.
     */
    ProposedContent draft(PlanChangePromptMessages messages);
}
