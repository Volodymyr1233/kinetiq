package com.kinetiq.plan.domain.service;

import com.kinetiq.plan.domain.model.GeneratedPlan;
import com.kinetiq.plan.domain.model.PlanPromptMessages;
import com.kinetiq.plan.domain.output.PlanGenerator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/** Gives its answers in order, each a plan or a thrown failure, and remembers every prompt. */
class StubPlanGenerator implements PlanGenerator {

    final List<PlanPromptMessages> received = new ArrayList<>();
    private final Deque<Supplier<GeneratedPlan>> answers = new ArrayDeque<>();

    StubPlanGenerator answering(GeneratedPlan plan) {
        answers.add(() -> plan);
        return this;
    }

    StubPlanGenerator failing(RuntimeException failure) {
        answers.add(() -> {
            throw failure;
        });
        return this;
    }

    @Override
    public GeneratedPlan generate(PlanPromptMessages messages) {
        received.add(messages);
        return answers.removeFirst().get();
    }
}
