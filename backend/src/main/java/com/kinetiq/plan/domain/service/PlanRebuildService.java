package com.kinetiq.plan.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.plan.domain.exception.PlanAlreadyPublishedException;
import com.kinetiq.plan.domain.exception.PlanDraftingInProgressException;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.exception.PlanRebuildLimitReachedException;
import com.kinetiq.plan.domain.input.RebuildPlanUseCase;
import com.kinetiq.plan.domain.model.DraftingKind;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.output.PlanBuildRequests;
import com.kinetiq.plan.domain.output.PlanDraftingRepository;
import com.kinetiq.plan.domain.output.PlanRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** Starts a new automatic draft over a stored one, within the trainer's daily limit, which protects the model budget. */
@RequiredArgsConstructor
public class PlanRebuildService implements RebuildPlanUseCase {

    static final int LIMIT = 10;
    static final Duration WINDOW = Duration.ofHours(24);

    private final ViewClientsUseCase clients;
    private final PlanRepository plans;
    private final PlanDraftingRepository draftings;
    private final PlanBuildRequests requests;
    private final Clock clock;

    // refusals come in the order of the data model, and only a request that passes them all is recorded
    @Override
    public void rebuild(AccountId trainer, AccountId client) {
        clients.getClient(trainer, client);
        Plan plan = plans.findByClient(client).orElseThrow(PlanNotFoundException::new);
        if (plan.isPublished()) {
            throw new PlanAlreadyPublishedException();
        }
        requests.lock(trainer);
        if (draftings.exists(client)) {
            throw new PlanDraftingInProgressException();
        }
        Instant now = clock.instant();
        List<Instant> counted = requests.since(trainer, now.minus(WINDOW));
        if (counted.size() >= LIMIT) {
            // the build that leaves the window then brings the count back under the limit
            throw new PlanRebuildLimitReachedException(counted.get(counted.size() - LIMIT).plus(WINDOW));
        }
        requests.record(trainer, now, now.minus(WINDOW));
        draftings.start(client, now, DraftingKind.REBUILD);
    }
}
