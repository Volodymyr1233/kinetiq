package com.kinetiq.plan.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.exception.GeneratedPlanRejectedException;
import com.kinetiq.plan.domain.exception.PlanGenerationFailedException;
import com.kinetiq.plan.domain.exception.PlanGeneratorUnavailableException;
import com.kinetiq.plan.domain.input.DraftInput;
import com.kinetiq.plan.domain.input.DraftPlanUseCase;
import com.kinetiq.plan.domain.input.PlanDrafting;
import com.kinetiq.plan.domain.model.GeneratedPlan;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.plan.domain.output.PlanGenerator;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Makes at most two attempts at a client's first plan and stores the result. Its log says how a drafting went and never
 * who it was for.
 */
@Slf4j
@RequiredArgsConstructor
public class PlanDraftingJob implements PlanDrafting {

    public static final int MAX_ATTEMPTS = 2;

    private final DraftPlanUseCase drafting;
    private final PlanGenerator generator;
    private final Duration pause;

    // a drafting left without a result is ended by the sweep
    @Override
    public void run(AccountId client) {
        long started = System.nanoTime();
        try {
            String outcome = draft(client);
            log.info("Plan drafting ended, outcome={}, duration={}", outcome, since(started));
        } catch (RuntimeException exception) {
            log.warn(
                    "Plan drafting stopped, cause={}, duration={}",
                    exception.getClass().getSimpleName(),
                    since(started));
        }
    }

    private String draft(AccountId client) {
        Optional<DraftInput> input = drafting.input(client);
        if (input.isEmpty()) {
            drafting.storeFailed(client);
            return "no-input";
        }
        PlanPrompt prompt = new PlanPrompt(input.get());
        List<String> rejections = List.of();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // a client who deleted their account meanwhile has no answers left, and nothing about them may be sent
            if (attempt > 1 && drafting.input(client).isEmpty()) {
                drafting.storeFailed(client);
                return "gone";
            }
            try {
                GeneratedPlan answer = generator.generate(prompt.messages(rejections));
                List<Session> sessions = GeneratedPlanChecks.check(answer, prompt.exerciseKeys(), input.get());
                drafting.storeBuilt(client, input.get().trainer(), sessions);
                return "built";
            } catch (GeneratedPlanRejectedException rejected) {
                rejections = rejected.codes();
                log.info("Plan attempt rejected, attempt={}, codes={}", attempt, rejections);
            } catch (PlanGenerationFailedException failed) {
                rejections = List.of();
                log.info("Plan attempt failed, attempt={}, reason={}", attempt, failed.getMessage());
            } catch (PlanGeneratorUnavailableException unavailable) {
                drafting.storeFailed(client);
                return "unavailable";
            }
            if (attempt < MAX_ATTEMPTS && !pause()) {
                break;
            }
        }
        drafting.storeFailed(client);
        return "failed";
    }

    private boolean pause() {
        try {
            Thread.sleep(pause);
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static Duration since(long started) {
        return Duration.ofNanos(System.nanoTime() - started);
    }
}
