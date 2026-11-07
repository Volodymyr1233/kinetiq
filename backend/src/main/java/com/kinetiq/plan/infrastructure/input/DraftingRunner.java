package com.kinetiq.plan.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.input.PlanDrafting;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Runs a drafting in the background; the caller starts it only once its drafting row is committed. */
@Component
@RequiredArgsConstructor
class DraftingRunner {

    private final PlanDrafting job;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    void start(AccountId client) {
        executor.execute(() -> job.run(client));
    }

    // does not wait for minutes of model calls: the sweep ends what this leaves behind
    @PreDestroy
    void destroy() {
        executor.shutdownNow();
    }
}
