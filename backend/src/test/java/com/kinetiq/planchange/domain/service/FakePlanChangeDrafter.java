package com.kinetiq.planchange.domain.service;

import com.kinetiq.planchange.domain.exception.PlanChangeDrafterUnavailableException;
import com.kinetiq.planchange.domain.model.PlanChangePromptMessages;
import com.kinetiq.planchange.domain.model.ProposedContent;
import com.kinetiq.planchange.domain.output.PlanChangeDrafter;
import com.kinetiq.planchange.domain.rule.HighRpeFallback;
import com.kinetiq.planchange.domain.rule.LowCompletionFallback;
import com.kinetiq.planchange.domain.rule.StalledWeightFallback;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/** Answers with what it was given, in order, and records every prompt; with nothing left it is unavailable. */
class FakePlanChangeDrafter implements PlanChangeDrafter {

    private final Deque<Supplier<ProposedContent>> answers = new ArrayDeque<>();
    final List<PlanChangePromptMessages> asked = new ArrayList<>();

    FakePlanChangeDrafter answering(ProposedContent answer) {
        answers.add(() -> answer);
        return this;
    }

    FakePlanChangeDrafter failing(RuntimeException exception) {
        answers.add(() -> {
            throw exception;
        });
        return this;
    }

    @Override
    public ProposedContent draft(PlanChangePromptMessages messages) {
        asked.add(messages);
        if (answers.isEmpty()) {
            throw new PlanChangeDrafterUnavailableException();
        }
        return answers.poll().get();
    }

    /** The drafting of the application, with this drafter. */
    PlanChangeDraftingService drafting() {
        return new PlanChangeDraftingService(
                this,
                PlanChangeLimitsTest.all(),
                List.of(new LowCompletionFallback(), new HighRpeFallback(), new StalledWeightFallback()));
    }
}
