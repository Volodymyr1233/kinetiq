package com.kinetiq.plan.domain.output;

import com.kinetiq.plan.domain.model.GeneratedPlan;
import com.kinetiq.plan.domain.model.PlanPromptMessages;

/** The gateway to the service that drafts a plan. One call is one attempt, and one request at most. */
public interface PlanGenerator {

    /**
     * Asks for a plan and returns the answer, still to be checked. Throws {@code PlanGenerationFailedException} when
     * this attempt gave no usable answer, and {@code PlanGeneratorUnavailableException} when no service is configured.
     */
    GeneratedPlan generate(PlanPromptMessages messages);
}
