package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.input.DetectPlanChangesUseCase;
import com.kinetiq.planchange.domain.model.ScheduledClient;
import com.kinetiq.planchange.domain.output.TrainingHistory;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlanChangeDetectionService implements DetectPlanChangesUseCase {

    private final TrainingHistory history;
    private final Clock clock;

    @Override
    public List<AccountId> due() {
        Instant now = clock.instant();
        return history.clientsWithPublishedPlan().stream()
                .filter(scheduled -> scheduled.isDue(now))
                .map(ScheduledClient::client)
                .toList();
    }

    @Override
    public void check(AccountId client) {
        Optional<ScheduledClient> scheduled = history.scheduledClient(client);
        Instant now = clock.instant();
        // the week is read again here: it may have been checked since due() ran
        if (scheduled.isEmpty()
                || !scheduled.get().isDue(now)
                || !history.claimWeek(client, scheduled.get().checkWeek(now), now)) {
            return;
        }
    }
}
