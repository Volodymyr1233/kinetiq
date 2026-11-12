package com.kinetiq.overview.domain.service;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.overview.domain.input.Overview;
import com.kinetiq.overview.domain.input.Overview.Alert;
import com.kinetiq.overview.domain.input.Overview.Kind;
import com.kinetiq.overview.domain.input.Overview.Row;
import com.kinetiq.overview.domain.input.Overview.Waiting;
import com.kinetiq.overview.domain.model.ClientStatus;
import com.kinetiq.overview.domain.model.ClientTraining;
import com.kinetiq.overview.domain.model.ClientTraining.DayCheckIn;
import com.kinetiq.overview.domain.model.ClientTraining.DayLog;
import com.kinetiq.overview.domain.model.ClientTraining.LastLog;
import com.kinetiq.overview.domain.model.Completion;
import com.kinetiq.overview.domain.model.PlanDates;
import com.kinetiq.overview.domain.model.RpeTotal;
import com.kinetiq.plan.domain.model.DraftedPlan;
import com.kinetiq.planchange.domain.input.PlanChangeApprovalView;
import com.kinetiq.planchange.domain.input.PlanChangeSummary;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.input.ViewPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.PlanChangeText;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.readiness.domain.model.Answers;
import com.kinetiq.readiness.domain.model.Readiness;
import com.kinetiq.readiness.domain.model.TargetChoice;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OverviewServiceTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    // Thursday of plan week 6
    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-11-12");
    private static final AccountId TRAINER = AccountId.random();

    // Monday, Wednesday and Friday from Wednesday 7 October, 8 weeks
    private static final PlanDates PLAN =
            new PlanDates(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-07"), 8, Set.of(MONDAY, WEDNESDAY, FRIDAY));

    // the 12 sessions due on Thursday 12 November: from Friday 16 October to yesterday
    private static final List<String> DUE = List.of(
            "2026-10-16", "2026-10-19", "2026-10-21", "2026-10-23", "2026-10-26", "2026-10-28",
            "2026-10-30", "2026-11-02", "2026-11-04", "2026-11-06", "2026-11-09", "2026-11-11");

    private final List<ClientSummary> clients = new ArrayList<>();
    private final Map<AccountId, ClientTraining> training = new HashMap<>();
    private final List<DraftedPlan> drafts = new ArrayList<>();
    private final List<PlanChangeApprovalView> approvals = new ArrayList<>();
    private final List<AccountId> asked = new ArrayList<>();
    private LocalDate since;

    private final OverviewService service = new OverviewService(
            new ViewClientsUseCase() {
                @Override
                public List<ClientSummary> listClients(AccountId trainer) {
                    asked.add(trainer);
                    return clients;
                }

                @Override
                public ClientSummary getClient(AccountId trainer, AccountId client) {
                    throw new UnsupportedOperationException();
                }
            },
            trainer -> {
                asked.add(trainer);
                return drafts;
            },
            new ViewPlanChangeUseCase() {
                @Override
                public List<PlanChangeApprovalView> approvals(AccountId trainer) {
                    asked.add(trainer);
                    return approvals;
                }

                @Override
                public PlanChangeView get(AccountId trainer, PlanChangeId id) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public PlanChangeSummary openOf(AccountId trainer, AccountId client) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public List<PlanChangeApprovalView> ended(AccountId trainer, AccountId client) {
                    throw new UnsupportedOperationException();
                }
            },
            (trainer, from) -> {
                asked.add(trainer);
                since = from;
                return training;
            },
            Clock.fixed(NOW, ZoneOffset.UTC));

    private AccountId client(String name, PlanState state, ClientTraining record) {
        AccountId id = AccountId.random();
        clients.add(new ClientSummary(id, name, Goal.BUILD_MUSCLE, state, WARSAW));
        if (record != null) {
            training.put(id, record);
        }
        return id;
    }

    // a running plan whose sessions on the given days were finished with the given RPE
    private static ClientTraining running(List<String> finished, int rpe, Map<LocalDate, DayCheckIn> checkIns) {
        Map<LocalDate, DayLog> days = new HashMap<>();
        finished.forEach(date -> days.put(LocalDate.parse(date), new DayLog(true, new RpeTotal(rpe * 3, 3))));
        String last = finished.isEmpty() ? null : finished.getLast();
        return new ClientTraining(
                PLAN,
                days,
                last == null
                        ? null
                        : new LastLog(Instant.parse(last + "T17:00:00Z"), LocalDate.parse(last), "Lower body"),
                checkIns);
    }

    private static ClientTraining running(List<String> finished, int rpe) {
        return running(finished, rpe, Map.of());
    }

    private void planChange(AccountId client, PlanChangeState state, Instant trainerAccepted, Instant clientAccepted) {
        ExerciseId squat = ExerciseId.random();
        PlanChange change = new PlanChange(
                PlanChangeId.random(),
                client,
                Rule.HIGH_RPE,
                state,
                1,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-08"),
                LocalDate.parse("2026-11-16"),
                PlanChangeContent.ofChanges(List.of(new TargetChange(
                        new EntryRef(MONDAY, 0), squat, "Lower", Target.AMOUNT, BigDecimal.TEN, BigDecimal.valueOf(9)))),
                List.of(),
                Set.of(squat),
                Instant.parse("2026-11-09T02:00:00Z"),
                state.isOpen() ? null : NOW,
                trainerAccepted,
                clientAccepted,
                null);
        approvals.add(new PlanChangeApprovalView(change, "Name", new PlanChangeText("Cut volume by 10%", "Reason")));
    }

    private Row row(Overview overview, AccountId client) {
        return overview.rows().stream()
                .filter(row -> row.client().getId().equals(client))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void shouldDecideEachClientsStatus_fromTheirPlanSessionsAndRpe() {
        AccountId atRisk = client("Anna", PlanState.PUBLISHED, running(DUE.subList(0, 7), 7));
        AccountId watch = client("Bartek", PlanState.PUBLISHED, running(DUE.subList(0, 8), 7));
        AccountId hard = client("Celina", PlanState.PUBLISHED, running(DUE, 9));
        AccountId onTrack = client("Dawid", PlanState.PUBLISHED, running(DUE, 8));
        AccountId draft = client("Ewa", PlanState.DRAFT_FAILED, null);
        AccountId noQuiz = client("Filip", PlanState.NO_QUIZ, null);

        Overview overview = service.overview(TRAINER);

        assertThat(row(overview, atRisk).completion()).isEqualTo(new Completion(7, 12));
        assertThat(overview.rows())
                .extracting(row -> row.verdict().status())
                .containsExactly(
                        ClientStatus.AT_RISK,
                        ClientStatus.WATCH,
                        ClientStatus.WATCH,
                        ClientStatus.ON_TRACK,
                        ClientStatus.PLAN_TO_REVIEW,
                        ClientStatus.NO_PLAN);
        assertThat(row(overview, hard).averageRpe()).isEqualByComparingTo("9.0");
        assertThat(row(overview, onTrack).week()).isEqualTo(6);
        assertThat(row(overview, draft).week()).isNull();
        assertThat(row(overview, noQuiz).completion()).isNull();
        assertThat(row(overview, watch).lastLoggedAt()).isEqualTo(Instant.parse("2026-11-02T17:00:00Z"));
    }

    @Test
    void shouldListTheAlertsMostSevereFirst_andByNameWithinAKind() {
        // the last 5 due sessions done, so there is no gap in the logs
        AccountId anna = client("Anna", PlanState.PUBLISHED, running(DUE.subList(7, 12), 7));
        AccountId bartek = client(
                "Bartek",
                PlanState.PUBLISHED,
                running(DUE, 9, Map.of(TODAY, new DayCheckIn(new Answers(1, 5, 5), TargetChoice.LIGHTER))));
        AccountId celina = client("Celina", PlanState.PUBLISHED, running(DUE, 7));
        AccountId dawid = client("Dawid", PlanState.PLAN_TO_REVIEW, null);
        AccountId ewa = client(
                "Ewa",
                PlanState.PUBLISHED,
                new ClientTraining(
                        new PlanDates(LocalDate.parse("2026-09-07"), LocalDate.parse("2026-09-07"), 8, Set.of(MONDAY)),
                        Map.of(),
                        null,
                        Map.of()));
        // the last session on Friday 6 November, 6 days ago: Monday and Wednesday since were missed
        AccountId filip = client("Filip", PlanState.PUBLISHED, running(DUE.subList(0, 10), 7));
        planChange(anna, PlanChangeState.ACCEPTED_BY_ONE, null, NOW);
        planChange(bartek, PlanChangeState.ACCEPTED_BY_ONE, NOW, null);
        planChange(celina, PlanChangeState.PENDING, null, null);
        planChange(dawid, PlanChangeState.EXPIRED, null, null);
        drafts.add(new DraftedPlan(dawid, "Dawid", DraftedPlan.Kind.BUILT, 8, 3, NOW));

        List<Alert> alerts = service.overview(TRAINER).alerts();

        assertThat(alerts)
                .extracting(Alert::kind, Alert::client)
                .containsExactly(
                        tuple(Kind.AT_RISK, anna),
                        tuple(Kind.WATCH, bartek),
                        tuple(Kind.NO_LOG, filip),
                        tuple(Kind.PLAN_CHANGE, celina),
                        tuple(Kind.DRAFT, dawid),
                        tuple(Kind.PLAN_ENDING, ewa),
                        tuple(Kind.RECOVERY_DAY, bartek));
        assertThat(alerts)
                .extracting(alert -> alert.planChange() == null ? null : alert.planChange().waitingFor())
                .containsExactly(Waiting.TRAINER, Waiting.CLIENT, null, Waiting.BOTH, null, null, null);
        assertThat(alerts.get(0).planChange().title()).isEqualTo("Cut volume by 10%");
        assertThat(alerts.get(2))
                .isEqualTo(new Alert(Kind.NO_LOG, filip, null, null, 6, LocalDate.parse("2026-11-06"), "Lower body"));
        assertThat(alerts.get(4).draft()).isEqualTo(DraftedPlan.Kind.BUILT);
    }

    @Test
    void shouldGiveNoLogAlert_after6DaysOnly_andCountFromThePublishDayWithoutAnySet() {
        // the last log on Saturday 7 November, 5 days ago
        List<String> logged = new ArrayList<>(DUE.subList(0, 10));
        logged.add("2026-11-07");
        AccountId recent = client("Anna", PlanState.PUBLISHED, running(logged, 7));
        AccountId never = client(
                "Bartek",
                PlanState.PUBLISHED,
                new ClientTraining(
                        new PlanDates(LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-06"), 8, Set.of(MONDAY)),
                        Map.of(),
                        null,
                        Map.of()));

        List<Alert> alerts = service.overview(TRAINER).alerts();

        assertThat(alerts).extracting(Alert::client).doesNotContain(recent);
        assertThat(alerts)
                .containsExactly(
                        new Alert(Kind.AT_RISK, never, null, null, null, null, null),
                        new Alert(Kind.NO_LOG, never, null, null, 6, LocalDate.parse("2026-11-06"), null));
    }

    @Test
    void shouldAlertThePlansEnd_inItsLastWeek_andKeepTheClientActive() {
        AccountId lastWeek = client(
                "Anna",
                PlanState.PUBLISHED,
                new ClientTraining(
                        new PlanDates(LocalDate.parse("2026-10-12"), LocalDate.parse("2026-10-12"), 5, Set.of()),
                        Map.of(),
                        new LastLog(NOW, TODAY, "Lower body"),
                        Map.of()));

        Overview overview = service.overview(TRAINER);

        assertThat(overview.alerts()).containsExactly(Alert.of(Kind.PLAN_ENDING, lastWeek));
        assertThat(overview.tiles().activeClients()).isEqualTo(1);
    }

    @Test
    void shouldCountTheTilesOverTheActiveClients() {
        // this week Monday 9 and Wednesday 11 November, last week Monday to Friday
        AccountId anna = client("Anna", PlanState.PUBLISHED, running(DUE, 8));
        client("Bartek", PlanState.PUBLISHED, running(DUE.subList(0, 10), 7));
        AccountId finished = client(
                "Celina",
                PlanState.PUBLISHED,
                new ClientTraining(
                        new PlanDates(LocalDate.parse("2026-09-07"), LocalDate.parse("2026-09-07"), 4, Set.of(MONDAY)),
                        Map.of(TODAY, new DayLog(true, new RpeTotal(30, 3))),
                        null,
                        Map.of()));
        client("Dawid", PlanState.NO_QUIZ, null);
        planChange(anna, PlanChangeState.ACCEPTED_BY_ONE, NOW, null);
        planChange(finished, PlanChangeState.PENDING, null, null);
        drafts.add(new DraftedPlan(AccountId.random(), "Ewa", DraftedPlan.Kind.FAILED, 8, 3, NOW));

        Overview.Tiles tiles = service.overview(TRAINER).tiles();

        assertThat(tiles.activeClients()).isEqualTo(2);
        assertThat(tiles.totalClients()).isEqualTo(4);
        assertThat(tiles.sessionsPlanned()).isEqualTo(6);
        assertThat(tiles.sessionsDone()).isEqualTo(2);
        // this week Anna's 2 days at 8; last week Anna's 3 days at 8 and Bartek's 3 at 7
        assertThat(tiles.averageRpe()).isEqualByComparingTo("8.0");
        assertThat(tiles.averageRpeChange()).isEqualByComparingTo("0.5");
        assertThat(tiles.draftsWaiting()).isEqualTo(1);
        // Anna's waits for her only
        assertThat(tiles.planChangesWaiting()).isEqualTo(1);
    }

    @Test
    void shouldHaveNoRpeTile_whenNothingWasLoggedThisWeek() {
        client("Anna", PlanState.PUBLISHED, running(DUE.subList(0, 9), 8));

        Overview.Tiles tiles = service.overview(TRAINER).tiles();

        assertThat(tiles.averageRpe()).isNull();
        assertThat(tiles.averageRpeChange()).isNull();
    }

    @Test
    void shouldShowTodaysReadiness_andWhetherTheLighterTargetsAreInUse() {
        AccountId lighter = client(
                "Anna",
                PlanState.PUBLISHED,
                running(DUE, 7, Map.of(TODAY, new DayCheckIn(new Answers(1, 5, 5), TargetChoice.LIGHTER))));
        AccountId original = client(
                "Bartek",
                PlanState.PUBLISHED,
                running(DUE, 7, Map.of(TODAY, new DayCheckIn(new Answers(1, 5, 5), TargetChoice.ORIGINAL))));
        AccountId ready = client(
                "Celina",
                PlanState.PUBLISHED,
                running(DUE, 7, Map.of(TODAY, new DayCheckIn(new Answers(5, 1, 1), TargetChoice.LIGHTER))));
        AccountId yesterday = client(
                "Dawid",
                PlanState.PUBLISHED,
                running(DUE, 7, Map.of(TODAY.minusDays(1), new DayCheckIn(new Answers(1, 5, 5), TargetChoice.LIGHTER))));

        Overview overview = service.overview(TRAINER);

        assertThat(row(overview, lighter).readiness()).isEqualTo(new Readiness(0, Readiness.Band.RECOVERY_DAY));
        assertThat(row(overview, lighter).lighterTargets()).isTrue();
        assertThat(row(overview, original).lighterTargets()).isFalse();
        assertThat(row(overview, ready).readiness().band()).isEqualTo(Readiness.Band.READY);
        assertThat(row(overview, ready).lighterTargets()).isFalse();
        assertThat(row(overview, yesterday).readiness()).isNull();
        assertThat(overview.alerts()).extracting(Alert::client).containsExactly(lighter, original);
    }

    @Test
    void shouldReadEnoughDaysBack_andOnlyForTheTrainer() {
        service.overview(TRAINER);

        // the 28 days of the completion end on a client's today, which can be a day behind the UTC date
        assertThat(since).isEqualTo(LocalDate.parse("2026-10-14"));
        assertThat(asked).containsOnly(TRAINER);
    }
}
