package com.kinetiq.plan.infrastructure.output;

import com.kinetiq.plan.domain.exception.PlanGeneratorUnavailableException;
import com.kinetiq.plan.domain.model.GeneratedPlan;
import com.kinetiq.plan.domain.model.PlanPromptMessages;
import com.kinetiq.plan.domain.output.PlanGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** Stands in when no key is configured, so the application runs and every draft ends as a failed one. */
@Slf4j
@Component
@ConditionalOnExpression("'${kinetiq.plan-drafting.api-key:}'.isBlank()")
class UnavailablePlanGenerator implements PlanGenerator {

    UnavailablePlanGenerator() {
        log.warn("No drafting service is configured, so every quiz submit gets an empty failed draft");
    }

    @Override
    public GeneratedPlan generate(PlanPromptMessages messages) {
        throw new PlanGeneratorUnavailableException();
    }
}
