package com.kinetiq.plan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.plan.domain.exception.PlanAlreadyPublishedException;
import com.kinetiq.plan.domain.exception.PlanDraftingInProgressException;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.exception.PlanRebuildLimitReachedException;
import com.kinetiq.plan.domain.model.DraftingKind;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanId;
import com.kinetiq.plan.domain.output.PlanBuildRequests;
import com.kinetiq.quiz.domain.model.Goal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanRebuildServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");

    private final AccountId trainer = AccountId.random();
    private final AccountId otherTrainer = AccountId.random();
    private final AccountId client = AccountId.random();
    private final InMemoryPlanRepository plans = new InMemoryPlanRepository();
    private final InMemoryPlanDraftingRepository draftings = new InMemoryPlanDraftingRepository();
    private final InMemoryBuildRequests requests = new InMemoryBuildRequests();

    // the client is only this trainer's
    private final ViewClientsUseCase clients = new ViewClientsUseCase() {
        @Override
        public List<ClientSummary> listClients(AccountId owner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientSummary getClient(AccountId owner, AccountId id) {
            if (!owner.equals(trainer) || !id.equals(client)) {
                throw new ClientNotFoundException();
            }
            return new ClientSummary(id, "Ola", Goal.LOSE_FAT, PlanState.PLAN_TO_REVIEW, ZoneId.of("Europe/Warsaw"));
        }
    };

    private static final class InMemoryBuildRequests implements PlanBuildRequests {

        record Request(AccountId trainer, Instant at) {}

        final List<Request> rows = new ArrayList<>();

        @Override
        public void lock(AccountId trainer) {}

        @Override
        public List<Instant> since(AccountId trainer, Instant from) {
            return rows.stream()
                    .filter(row -> row.trainer().equals(trainer) && row.at().isAfter(from))
                    .map(Request::at)
                    .sorted()
                    .toList();
        }

        @Override
        public void record(AccountId trainer, Instant at, Instant forgetBefore) {
            rows.removeIf(row -> row.trainer().equals(trainer) && row.at().isBefore(forgetBefore));
            rows.add(new Request(trainer, at));
        }
    }

    private PlanRebuildService serviceAt(Instant now) {
        return new PlanRebuildService(clients, plans, draftings, requests, Clock.fixed(now, ZoneOffset.UTC));
    }

    private void storeDraft() {
        plans.save(new Plan(PlanId.random(), client, Goal.LOSE_FAT, 12, null, List.of(), 0));
    }

    private void usedBefore(AccountId by, Instant... times) {
        for (Instant at : times) {
            requests.rows.add(new InMemoryBuildRequests.Request(by, at));
        }
    }

    // nine requests an hour apart, the oldest nine hours ago
    private void usedNine() {
        for (int hours = 9; hours >= 1; hours--) {
            usedBefore(trainer, NOW.minus(Duration.ofHours(hours)));
        }
    }

    @Test
    void shouldStartARebuildAndCountIt() {
        storeDraft();

        serviceAt(NOW).rebuild(trainer, client);

        assertThat(draftings.rows.get(client).kind()).isEqualTo(DraftingKind.REBUILD);
        assertThat(draftings.rows.get(client).startedAt()).isEqualTo(NOW);
        assertThat(requests.since(trainer, Instant.EPOCH)).containsExactly(NOW);
    }

    @Test
    void shouldRefuseAsClientNotFound_forAnotherTrainersOrAnUnknownClient() {
        storeDraft();

        assertThatThrownBy(() -> serviceAt(NOW).rebuild(otherTrainer, client))
                .isInstanceOf(ClientNotFoundException.class);
        assertThatThrownBy(() -> serviceAt(NOW).rebuild(trainer, AccountId.random()))
                .isInstanceOf(ClientNotFoundException.class);
        assertThat(requests.rows).isEmpty();
        assertThat(draftings.rows).isEmpty();
    }

    @Test
    void shouldRefuseAsPlanNotFound_whenThereIsNoPlan() {
        assertThatThrownBy(() -> serviceAt(NOW).rebuild(trainer, client)).isInstanceOf(PlanNotFoundException.class);
        assertThat(requests.rows).isEmpty();
    }

    @Test
    void shouldRefuseAsAlreadyPublished_forAPublishedPlan() {
        plans.save(new Plan(PlanId.random(), client, Goal.LOSE_FAT, 12, LocalDate.parse("2026-11-02"), List.of(), 0));

        assertThatThrownBy(() -> serviceAt(NOW).rebuild(trainer, client))
                .isInstanceOf(PlanAlreadyPublishedException.class);
        assertThat(requests.rows).isEmpty();
    }

    @Test
    void shouldRefuseASecondRequestWhileDrafting_withoutCountingIt() {
        storeDraft();
        serviceAt(NOW).rebuild(trainer, client);

        assertThatThrownBy(() -> serviceAt(NOW.plusSeconds(1)).rebuild(trainer, client))
                .isInstanceOf(PlanDraftingInProgressException.class);
        assertThat(requests.rows).hasSize(1);
    }

    // a drafting refusal comes before the limit, so a trainer at the limit learns the real reason
    @Test
    void shouldRefuseAsDrafting_beforeLookingAtTheLimit() {
        storeDraft();
        usedNine();
        usedBefore(trainer, NOW.minusSeconds(60));
        draftings.start(client, NOW, DraftingKind.REBUILD);

        assertThatThrownBy(() -> serviceAt(NOW).rebuild(trainer, client))
                .isInstanceOf(PlanDraftingInProgressException.class);
    }

    @Test
    void shouldAcceptTheTenthAndRefuseTheEleventh_sayingWhenTheOldestLeavesTheWindow() {
        storeDraft();
        usedNine();

        serviceAt(NOW).rebuild(trainer, client);
        draftings.remove(client);

        assertThatThrownBy(() -> serviceAt(NOW.plusSeconds(1)).rebuild(trainer, client))
                .isInstanceOfSatisfying(PlanRebuildLimitReachedException.class, refused -> {
                    assertThat(refused.code()).isEqualTo("plan.rebuild-limit-reached");
                    assertThat(refused.properties())
                            .containsEntry("availableAt", NOW.minus(Duration.ofHours(9)).plus(Duration.ofHours(24)).toString());
                });
        assertThat(requests.rows).hasSize(10);
        assertThat(draftings.rows).isEmpty();
    }

    @Test
    void shouldAcceptAgain_fromTheMomentTheOldestRequestLeavesTheWindow() {
        storeDraft();
        usedNine();
        serviceAt(NOW).rebuild(trainer, client);
        draftings.remove(client);
        Instant availableAt = NOW.minus(Duration.ofHours(9)).plus(Duration.ofHours(24));

        assertThatThrownBy(() -> serviceAt(availableAt.minusSeconds(1)).rebuild(trainer, client))
                .isInstanceOf(PlanRebuildLimitReachedException.class);
        serviceAt(availableAt).rebuild(trainer, client);

        assertThat(draftings.rows).containsKey(client);
    }

    @Test
    void shouldNotCountAnotherTrainersRequests() {
        storeDraft();
        for (int minutes = 1; minutes <= 20; minutes++) {
            usedBefore(otherTrainer, NOW.minus(Duration.ofMinutes(minutes)));
        }

        serviceAt(NOW).rebuild(trainer, client);

        assertThat(draftings.rows).containsKey(client);
    }

    @Test
    void shouldForgetRequestsOlderThanTheWindow_whenRecording() {
        storeDraft();
        usedBefore(trainer, NOW.minus(Duration.ofHours(30)));

        serviceAt(NOW).rebuild(trainer, client);

        assertThat(requests.rows).extracting(InMemoryBuildRequests.Request::at).containsExactly(NOW);
    }
}
