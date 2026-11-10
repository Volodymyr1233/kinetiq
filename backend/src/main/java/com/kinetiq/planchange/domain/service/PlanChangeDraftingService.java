package com.kinetiq.planchange.domain.service;

import com.kinetiq.planchange.domain.exception.PlanChangeContentRejectedException;
import com.kinetiq.planchange.domain.exception.PlanChangeDrafterUnavailableException;
import com.kinetiq.planchange.domain.exception.PlanChangeDraftingFailedException;
import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.output.PlanChangeDrafter;
import com.kinetiq.planchange.domain.rule.FixedTransformation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds the content of a due plan change: asks the drafting service at most twice, keeps the first answer within the
 * limits, and otherwise uses the rule's fixed transformation. Its log holds rules, attempts and limit codes, never a
 * prompt, an answer or a person.
 */
@Slf4j
public class PlanChangeDraftingService {

    static final int MAX_ATTEMPTS = 2;

    private final PlanChangeDrafter drafter;
    private final PlanChangeLimits limits;
    private final Map<Rule, FixedTransformation> fallbacks;

    public PlanChangeDraftingService(
            PlanChangeDrafter drafter, PlanChangeLimits limits, List<FixedTransformation> fallbacks) {
        this.drafter = drafter;
        this.limits = limits;
        this.fallbacks =
                fallbacks.stream().collect(Collectors.toUnmodifiableMap(FixedTransformation::rule, Function.identity()));
        for (Rule rule : Rule.values()) {
            if (!this.fallbacks.containsKey(rule)) {
                throw new IllegalArgumentException("no fixed transformation for " + rule);
            }
        }
    }

    /** The checked content for the due plan change; empty when even the fixed transformation changes nothing. */
    public Optional<PlanChangeContent> draft(PlanSnapshot plan, DuePlanChange due) {
        PlanChangePrompt prompt = new PlanChangePrompt(plan, due);
        Set<String> rejections = Set.of();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                PlanChangeContent content = prompt.toContent(drafter.draft(prompt.messages(rejections)));
                rejections = limits.violations(plan, due, content);
                if (rejections.isEmpty()) {
                    log.info("Plan change content drafted, rule={}, attempt={}", due.rule(), attempt);
                    return Optional.of(content);
                }
            } catch (PlanChangeContentRejectedException rejected) {
                rejections = rejected.codes();
            } catch (PlanChangeDraftingFailedException failed) {
                rejections = Set.of();
                log.info("Plan change attempt failed, rule={}, attempt={}", due.rule(), attempt);
                continue;
            } catch (PlanChangeDrafterUnavailableException unavailable) {
                break;
            }
            log.info("Plan change attempt refused, rule={}, attempt={}, codes={}", due.rule(), attempt, rejections);
        }
        Optional<PlanChangeContent> fixed = fallbacks.get(due.rule()).apply(plan, due);
        log.info("Plan change content fixed, rule={}, empty={}", due.rule(), fixed.isEmpty());
        return fixed;
    }
}
