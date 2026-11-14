package com.kinetiq.sessionlog.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.ListExercisesQuery;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.input.DraftSummary;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.progress.domain.input.Progress;
import com.kinetiq.progress.domain.input.Rewards;
import com.kinetiq.progress.domain.input.ViewProgressUseCase;
import com.kinetiq.progress.domain.model.Period;
import com.kinetiq.progress.domain.model.Pr;
import com.kinetiq.progress.domain.model.TopSet;
import com.kinetiq.readiness.domain.input.CheckInUseCase;
import com.kinetiq.readiness.domain.input.ChooseTargetsCommand;
import com.kinetiq.readiness.domain.input.SaveCheckInCommand;
import com.kinetiq.readiness.domain.input.TodayCheckIn;
import com.kinetiq.readiness.domain.model.Adjustment;
import com.kinetiq.readiness.domain.model.Answers;
import com.kinetiq.readiness.domain.model.CheckIn;
import com.kinetiq.readiness.domain.model.Planned;
import com.kinetiq.account.domain.model.WeightUnit;
import com.kinetiq.sessionlog.domain.exception.NoSessionTodayException;
import com.kinetiq.sessionlog.domain.exception.NotFinishedException;
import com.kinetiq.sessionlog.domain.exception.NothingLoggedException;
import com.kinetiq.sessionlog.domain.exception.SessionLogLockedException;
import com.kinetiq.sessionlog.domain.exception.SessionLogNotFoundException;
import com.kinetiq.sessionlog.domain.exception.SetValuesInvalidException;
import com.kinetiq.sessionlog.domain.input.LogSetCommand;
import com.kinetiq.sessionlog.domain.input.SessionSummaryView;
import com.kinetiq.sessionlog.domain.input.StartedSession;
import com.kinetiq.sessionlog.domain.model.LoggedEntry;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogHeader;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLog;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import com.kinetiq.sessionlog.domain.model.SetValues;
import com.kinetiq.sessionlog.domain.output.SessionLogRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SessionLogServiceTest {

    private static final AccountId TRAINER = AccountId.random();
    private static final AccountId CLIENT = AccountId.random();
    private static final AccountId OTHER_CLIENT = AccountId.random();
    private static final LocalDate TODAY = LocalDate.parse("2026-11-07");
    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");

    private static final Exercise SQUAT = new Exercise(
            ExerciseId.random(),
            TRAINER,
            new LocalizedText("Squat", "Przysiad"),
            Category.LEGS,
            new LocalizedText("", ""),
            Measure.REPS,
            3,
            5,
            120,
            new LocalizedText("", ""));

    private static final Exercise PLANK = new Exercise(
            ExerciseId.random(),
            TRAINER,
            new LocalizedText("Plank", "Deska"),
            Category.CORE,
            new LocalizedText("", ""),
            Measure.SECONDS,
            3,
            45,
            90,
            new LocalizedText("", ""));

    private TodayView today = sessionDay("Lower body");
    private RuntimeException planFailure;
    private final Map<SessionLogId, SessionLog> stored = new LinkedHashMap<>();
    // the answers each client gave on a date, and whether they chose the lighter targets
    private final Map<List<Object>, Answers> answers = new HashMap<>();
    private boolean lighterInUse = true;

    private static TodayView sessionDay(String name) {
        Session session = new Session(
                name,
                DayOfWeek.SATURDAY,
                List.of(new ExerciseEntry(SQUAT.getId(), 3, 5, new BigDecimal("100.0"), 8, 120)));
        return new TodayView(TODAY, TodayView.State.SESSION, session, Map.of(SQUAT.getId(), SQUAT));
    }

    private final ViewOwnPlanUseCase plan = new ViewOwnPlanUseCase() {
        @Override
        public OwnPlanView view(AccountId client, Integer week) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void acknowledge(AccountId client) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DraftSummary draftSummary(AccountId client) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TodayView today(AccountId client) {
            if (planFailure != null) {
                throw planFailure;
            }
            return today;
        }
    };

    private final CurrentAccountUseCase accounts = new CurrentAccountUseCase() {
        @Override
        public Account find(AccountId id) {
            return Account.restore(
                    id,
                    "Ola",
                    new Email("ola@example.com"),
                    "hash",
                    Role.CLIENT,
                    TRAINER,
                    ZoneId.of("Europe/Warsaw"),
                    AccountSettings.startingWith(Language.EN),
                    true,
                    null,
                    null,
                    null,
                    NOW);
        }

        @Override
        public void changeTimeZone(AccountId id, ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Account update(AccountId id, UpdateAccountCommand command) {
            throw new UnsupportedOperationException();
        }
    };

    private final ListExercisesUseCase exercises = new ListExercisesUseCase() {
        @Override
        public ExerciseListing list(ListExercisesQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Exercise> library(AccountId trainer) {
            return List.of(SQUAT, PLANK);
        }
    };

    private final SessionLogRepository logs = new SessionLogRepository() {
        @Override
        public Optional<SessionLog> findByClientAndDate(AccountId client, LocalDate date) {
            return stored.values().stream()
                    .filter(log -> log.getClient().equals(client) && log.getDate().equals(date))
                    .findFirst();
        }

        @Override
        public Optional<SessionLog> findByIdAndClient(SessionLogId id, AccountId client) {
            return Optional.ofNullable(stored.get(id)).filter(log -> log.getClient().equals(client));
        }

        @Override
        public Optional<SessionLogId> sessionOfSetLog(SetLogId id) {
            return stored.values().stream()
                    .filter(log -> log.getSetLogs().stream().anyMatch(set -> set.id().equals(id)))
                    .map(SessionLog::getId)
                    .findFirst();
        }

        @Override
        public List<SessionLogHeader> headersBetween(AccountId client, LocalDate from, LocalDate to) {
            return stored.values().stream()
                    .filter(log -> log.getClient().equals(client)
                            && !log.getDate().isBefore(from)
                            && !log.getDate().isAfter(to))
                    .map(log -> new SessionLogHeader(
                            log.getId(), log.getDate(), log.getStartedAt(), log.getFinishedAt().orElse(null)))
                    .toList();
        }

        @Override
        public List<SessionLog> findAllByClient(AccountId client) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SessionLog save(SessionLog log) {
            stored.put(log.getId(), log);
            return log;
        }
    };

    // the real rule over the answers given here, so a set copies what the client was asked that day
    private final CheckInUseCase checkIns = new CheckInUseCase() {
        @Override
        public TodayCheckIn today(AccountId client) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TodayCheckIn save(SaveCheckInCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TodayCheckIn choose(ChooseTargetsCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Adjustment> adjustmentOn(AccountId client, LocalDate date, List<Planned> entries) {
            return Optional.ofNullable(answers.get(List.of(client, date)))
                    .flatMap(given -> Adjustment.of(given, lighterInUse, entries, WeightUnit.KG));
        }

        @Override
        public List<CheckIn> between(AccountId client, LocalDate from, LocalDate to) {
            throw new UnsupportedOperationException();
        }
    };

    // the rewards given for any date, and the dates they were asked for
    private Rewards rewards = new Rewards(List.of(), 0);
    private final List<LocalDate> rewardDates = new ArrayList<>();

    private final ViewProgressUseCase progress = new ViewProgressUseCase() {
        @Override
        public Progress own(AccountId client, Period period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Progress ofClient(AccountId trainer, AccountId client, Period period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Rewards rewards(AccountId client, LocalDate date) {
            rewardDates.add(date);
            return rewards;
        }
    };

    private final SessionLogService service = new SessionLogService(
            plan, accounts, exercises, logs, checkIns, progress, Clock.fixed(NOW, ZoneOffset.UTC));

    private static LogSetCommand command(
            AccountId client, SessionLogId session, SetLogId id, int position, int number, int amount, int rpe) {
        return new LogSetCommand(client, session, id, position, number, new BigDecimal("100.0"), amount, rpe);
    }

    private static SetLogId newId() {
        return new SetLogId(UUID.randomUUID());
    }

    private SessionLogId startedAt(Instant start, AccountId client) {
        SessionLog log = SessionLog.start(
                client,
                TODAY.minusDays(1),
                "Lower body",
                List.of(new LoggedEntry(SQUAT.getId(), 3, 5, new BigDecimal("100.0"), 8, 120)),
                start);
        return logs.save(log).getId();
    }

    @Nested
    class Start {

        @Test
        void shouldStartTodaysSessionWithACopyOfThePlannedOne() {
            StartedSession started = service.start(CLIENT);

            assertThat(started.created()).isTrue();
            assertThat(started.log().getDate()).isEqualTo(TODAY);
            assertThat(started.log().getName()).isEqualTo("Lower body");
            assertThat(started.log().getEntries()).hasSize(1);
            assertThat(started.log().getEntries().getFirst().sets()).isEqualTo(3);
            assertThat(started.library()).containsKey(SQUAT.getId());
            assertThat(stored).hasSize(1);
        }

        @Test
        void shouldReturnTheSameSession_whenItWasStartedAlreadyToday() {
            StartedSession first = service.start(CLIENT);

            StartedSession again = service.start(CLIENT);

            assertThat(again.created()).isFalse();
            assertThat(again.log().getId()).isEqualTo(first.log().getId());
            assertThat(stored).hasSize(1);
        }

        @Test
        void shouldKeepTheStartedCopy_whenThePlanChangesAfterwards() {
            service.start(CLIENT);
            today = sessionDay("Heavy lower");

            StartedSession again = service.start(CLIENT);

            assertThat(again.log().getName()).isEqualTo("Lower body");
        }

        @Test
        void shouldRefuse_whenTodayIsARestDayOrThePlanHasFinished() {
            today = new TodayView(TODAY, TodayView.State.REST, null, Map.of());
            assertThatThrownBy(() -> service.start(CLIENT)).isInstanceOf(NoSessionTodayException.class);

            today = new TodayView(TODAY, TodayView.State.FINISHED, null, Map.of());
            assertThatThrownBy(() -> service.start(CLIENT)).isInstanceOf(NoSessionTodayException.class);

            assertThat(stored).isEmpty();
        }

        @Test
        void shouldAnswerNotFound_whenThereIsNoPublishedPlan() {
            planFailure = new PlanNotFoundException();

            assertThatThrownBy(() -> service.start(CLIENT)).isExactlyInstanceOf(PlanNotFoundException.class);
        }
    }

    @Nested
    class Find {

        @Test
        void shouldGiveTheClientsSessionOfAnEarlierDay_withoutStartingTodays() {
            SessionLogId yesterdays = startedAt(NOW.minus(Duration.ofHours(20)), CLIENT);

            StartedSession found = service.find(CLIENT, yesterdays);

            assertThat(found.log().getId()).isEqualTo(yesterdays);
            assertThat(found.created()).isFalse();
            assertThat(found.library()).containsKey(SQUAT.getId());
            assertThat(stored).hasSize(1);
        }

        @Test
        void shouldAnswerNotFound_whenTheSessionIsAnotherClientsOrUnknown() {
            SessionLogId theirs = startedAt(NOW.minus(Duration.ofHours(20)), OTHER_CLIENT);

            assertThatThrownBy(() -> service.find(CLIENT, theirs)).isInstanceOf(SessionLogNotFoundException.class);
            assertThatThrownBy(() -> service.find(CLIENT, SessionLogId.random()))
                    .isInstanceOf(SessionLogNotFoundException.class);
        }
    }

    @Nested
    class OnARecoveryDay {

        @Test
        void shouldStartWithTheDaysAdjustment() {
            answers.put(List.of(CLIENT, TODAY), new Answers(2, 4, 3));

            StartedSession started = service.start(CLIENT);

            assertThat(started.adjustment().inUse()).isTrue();
        }

        @Test
        void shouldStartWithoutAnAdjustment_whenTheClientDidNotCheckIn() {
            assertThat(service.start(CLIENT).adjustment()).isNull();
        }

        @Test
        void shouldCopyTheLighterTargets_intoASetLoggedOnThatDay() {
            answers.put(List.of(CLIENT, TODAY), new Answers(2, 4, 3));
            SessionLogId session = service.start(CLIENT).log().getId();

            SetLog logged = service.logSet(new LogSetCommand(
                    CLIENT, session, newId(), 0, 1, new BigDecimal("90.0"), 5, 7));

            assertThat(logged.targetLoadKg()).isEqualByComparingTo("90.0");
            assertThat(logged.targetAmount()).isEqualTo(5);
            assertThat(logged.targetRpe()).isEqualTo(7);
            // the plan's copy in the session stays as planned
            assertThat(stored.get(session).getEntries().getFirst().loadKg()).isEqualByComparingTo("100.0");
            assertThat(stored.get(session).getEntries().getFirst().rpe()).isEqualTo(8);
        }

        @Test
        void shouldAnswerNotFound_forAnotherClientsAdjustedSession() {
            answers.put(List.of(OTHER_CLIENT, TODAY.minusDays(1)), new Answers(2, 4, 3));
            SessionLogId theirs = startedAt(NOW.minus(Duration.ofHours(20)), OTHER_CLIENT);

            assertThatThrownBy(() -> service.find(CLIENT, theirs)).isInstanceOf(SessionLogNotFoundException.class);
        }

        @Test
        void shouldCopyThePlannedTargets_whenTheClientChoseTheOriginalOnes() {
            answers.put(List.of(CLIENT, TODAY), new Answers(2, 4, 3));
            lighterInUse = false;
            SessionLogId session = service.start(CLIENT).log().getId();

            SetLog logged = service.logSet(command(CLIENT, session, newId(), 0, 1, 5, 8));

            assertThat(logged.targetLoadKg()).isEqualByComparingTo("100.0");
            assertThat(logged.targetRpe()).isEqualTo(8);
        }
    }

    @Nested
    class LogSet {

        @Test
        void shouldLogASetAsPlanned_andStoreItOnce_whenTheIdIsSentTwice() {
            SessionLogId session = service.start(CLIENT).log().getId();
            SetLogId id = newId();

            SetLog first = service.logSet(command(CLIENT, session, id, 0, 1, 5, 8));
            SetLog again = service.logSet(command(CLIENT, session, id, 0, 1, 5, 8));

            assertThat(again).isEqualTo(first);
            assertThat(stored.get(session).getSetLogs()).hasSize(1);
            assertThat(first.targetAmount()).isEqualTo(5);
        }

        @Test
        void shouldChangeALoggedSet_whenTheSameIdIsSentWithNewValues() {
            SessionLogId session = service.start(CLIENT).log().getId();
            SetLogId id = newId();
            service.logSet(command(CLIENT, session, id, 0, 1, 5, 8));

            SetLog changed = service.logSet(command(CLIENT, session, id, 0, 1, 6, 9));

            assertThat(changed.values().amount()).isEqualTo(6);
            assertThat(stored.get(session).getSetLogs()).hasSize(1);
        }

        @Test
        void shouldAnswerNotFound_whenTheSessionIsAnotherClientsOrUnknown() {
            SessionLogId session = service.start(CLIENT).log().getId();

            assertThatThrownBy(() -> service.logSet(command(OTHER_CLIENT, session, newId(), 0, 1, 5, 8)))
                    .isInstanceOf(SessionLogNotFoundException.class);
            assertThatThrownBy(() -> service.logSet(command(CLIENT, SessionLogId.random(), newId(), 0, 1, 5, 8)))
                    .isInstanceOf(SessionLogNotFoundException.class);
            assertThat(stored.get(session).getSetLogs()).isEmpty();
        }

        @Test
        void shouldAnswerNotFoundAndLeaveTheSetAlone_whenTheIdBelongsToAnotherSession() {
            SessionLogId mine = service.start(CLIENT).log().getId();
            SessionLogId theirs = service.start(OTHER_CLIENT).log().getId();
            SetLogId theirSet = newId();
            service.logSet(command(OTHER_CLIENT, theirs, theirSet, 0, 1, 5, 8));

            assertThatThrownBy(() -> service.logSet(command(CLIENT, mine, theirSet, 0, 1, 9, 9)))
                    .isInstanceOf(SessionLogNotFoundException.class);

            assertThat(stored.get(theirs).getSetLogs().getFirst().values().amount()).isEqualTo(5);
            assertThat(stored.get(mine).getSetLogs()).isEmpty();
        }

        @Test
        void shouldStoreNothing_whenAValueIsOutsideItsLimits() {
            SessionLogId session = service.start(CLIENT).log().getId();

            assertThatThrownBy(() -> service.logSet(command(CLIENT, session, newId(), 0, 1, 0, 8)))
                    .isInstanceOf(SetValuesInvalidException.class);
            assertThatThrownBy(() -> service.logSet(command(CLIENT, session, newId(), 0, 1, 5, 4)))
                    .isInstanceOf(SetValuesInvalidException.class);
            assertThatThrownBy(() -> service.logSet(command(CLIENT, session, newId(), 0, 4, 5, 8)))
                    .isInstanceOf(SetValuesInvalidException.class);

            assertThat(stored.get(session).getSetLogs()).isEmpty();
        }

        @Test
        void shouldTakeASet_whenTheSessionStartedASecondLessThan24HoursAgo() {
            SessionLogId session = startedAt(NOW.minus(Duration.ofHours(24)).plusSeconds(1), CLIENT);

            service.logSet(command(CLIENT, session, newId(), 0, 1, 5, 8));

            assertThat(stored.get(session).getSetLogs()).hasSize(1);
        }

        @Test
        void shouldRefuseASetAndStoreNothing_whenTheSessionStarted24HoursAgo() {
            SessionLogId session = startedAt(NOW.minus(Duration.ofHours(24)), CLIENT);

            assertThatThrownBy(() -> service.logSet(command(CLIENT, session, newId(), 0, 1, 5, 8)))
                    .isInstanceOf(SessionLogLockedException.class);

            assertThat(stored.get(session).getSetLogs()).isEmpty();
        }

        @Test
        void shouldRefuseARepeatWithNewValuesAndChangeNothing_whenTheSessionLockedAfterTheSetWasStored() {
            SetLogId id = newId();
            SessionLog log = new SessionLog(
                    SessionLogId.random(),
                    CLIENT,
                    TODAY.minusDays(2),
                    "Lower body",
                    NOW.minus(Duration.ofHours(30)),
                    null,
                    List.of(new LoggedEntry(SQUAT.getId(), 1, 5, new BigDecimal("100.0"), 8, 120)),
                    List.of(new SetLog(
                            id, 0, 1, new SetValues(new BigDecimal("100.0"), 5, 8), new BigDecimal("100.0"), 5, 8,
                            NOW.minus(Duration.ofHours(29)))),
                    0);
            logs.save(log);

            assertThatThrownBy(() -> service.logSet(command(CLIENT, log.getId(), id, 0, 1, 9, 10)))
                    .isInstanceOf(SessionLogLockedException.class);

            assertThat(stored.get(log.getId()).getSetLogs()).hasSize(1);
            assertThat(stored.get(log.getId()).getSetLogs().getFirst().values().amount()).isEqualTo(5);
        }

        @Test
        void shouldAnswerNotFound_whenALockedSessionIsAnotherClients() {
            SessionLogId theirs = startedAt(NOW.minus(Duration.ofHours(25)), OTHER_CLIENT);

            assertThatThrownBy(() -> service.logSet(command(CLIENT, theirs, newId(), 0, 1, 5, 8)))
                    .isInstanceOf(SessionLogNotFoundException.class);
        }
    }

    @Nested
    class Finish {

        private SessionLogId withOneSet(AccountId client) {
            SessionLogId session = service.start(client).log().getId();
            service.logSet(command(client, session, newId(), 0, 1, 5, 8));
            return session;
        }

        private SessionLog finishedOn(LocalDate date) {
            Instant finishedAt = date.atTime(18, 0).toInstant(ZoneOffset.UTC);
            return logs.save(new SessionLog(
                    SessionLogId.random(),
                    CLIENT,
                    date,
                    "Lower body",
                    finishedAt.minus(Duration.ofHours(1)),
                    finishedAt,
                    List.of(new LoggedEntry(SQUAT.getId(), 1, 5, new BigDecimal("100.0"), 8, 120)),
                    List.of(new SetLog(
                            newId(), 0, 1, new SetValues(new BigDecimal("100.0"), 5, 8), new BigDecimal("100.0"), 5, 8,
                            finishedAt.minus(Duration.ofMinutes(30)))),
                    0));
        }

        @Test
        void shouldStoreTheFinishTimeAndAnswerTheSummary() {
            SessionLogId session = withOneSet(CLIENT);

            SessionSummaryView view = service.finish(CLIENT, session);

            assertThat(stored.get(session).getFinishedAt()).contains(NOW);
            assertThat(view.summary().setsLogged()).isEqualTo(1);
            assertThat(view.summary().setsPlanned()).isEqualTo(3);
            assertThat(view.summary().volumeKg()).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("500.0"));
            assertThat(view.summary().minutes()).isEqualTo(1);
        }

        @Test
        void shouldAnswerThePrsInTheSessionsOrderAndTheStreak_whenFinishedAndReadAgain() {
            today = new TodayView(
                    TODAY,
                    TodayView.State.SESSION,
                    new Session(
                            "Full body",
                            DayOfWeek.SATURDAY,
                            List.of(
                                    new ExerciseEntry(SQUAT.getId(), 3, 5, new BigDecimal("100.0"), 8, 120),
                                    new ExerciseEntry(PLANK.getId(), 3, 45, null, 7, 90))),
                    Map.of(SQUAT.getId(), SQUAT, PLANK.getId(), PLANK));
            TopSet squat = new TopSet(new BigDecimal("105.0"), 5, 9);
            TopSet plank = new TopSet(null, 60, 7);
            rewards = new Rewards(List.of(new Pr(PLANK.getId(), plank), new Pr(SQUAT.getId(), squat)), 4);
            SessionLogId session = withOneSet(CLIENT);

            SessionSummaryView finished = service.finish(CLIENT, session);
            SessionSummaryView read = service.summary(CLIENT, session);

            assertThat(finished.prs())
                    .containsExactly(
                            new SessionSummaryView.ExercisePr(SQUAT, squat),
                            new SessionSummaryView.ExercisePr(PLANK, plank));
            assertThat(finished.weeksInARow()).isEqualTo(4);
            assertThat(read.prs()).isEqualTo(finished.prs());
            assertThat(read.weeksInARow()).isEqualTo(4);
        }

        @Test
        void shouldAskForTheRewardsOfTheSessionsDate_whenItsSummaryIsReadOnALaterDay() {
            SessionLog earlier = finishedOn(TODAY.minusDays(3));

            service.summary(CLIENT, earlier.getId());

            assertThat(rewardDates).containsExactly(TODAY.minusDays(3));
        }

        @Test
        void shouldAnswerTheSameSummary_whenFinishedTwice() {
            SessionLogId session = withOneSet(CLIENT);

            SessionSummaryView first = service.finish(CLIENT, session);
            SessionSummaryView again = service.finish(CLIENT, session);

            assertThat(again.summary()).isEqualTo(first.summary());
            assertThat(again.log().getFinishedAt()).isEqualTo(first.log().getFinishedAt());
        }

        @Test
        void shouldAnswerNotFoundAndChangeNothing_whenTheSessionIsAnotherClientsOrUnknown() {
            SessionLogId theirs = withOneSet(OTHER_CLIENT);
            SessionLogId lockedTheirs = startedAt(NOW.minus(Duration.ofHours(30)), OTHER_CLIENT);

            for (SessionLogId id : List.of(theirs, lockedTheirs, SessionLogId.random())) {
                assertThatThrownBy(() -> service.finish(CLIENT, id)).isInstanceOf(SessionLogNotFoundException.class);
                assertThatThrownBy(() -> service.summary(CLIENT, id)).isInstanceOf(SessionLogNotFoundException.class);
            }
            assertThat(stored.get(theirs).getFinishedAt()).isEmpty();
        }

        @Test
        void shouldRefuseWithNothingLogged_andWithLocked() {
            SessionLogId empty = service.start(CLIENT).log().getId();
            SessionLogId locked = startedAt(NOW.minus(Duration.ofHours(24)), CLIENT);

            assertThatThrownBy(() -> service.finish(CLIENT, empty)).isInstanceOf(NothingLoggedException.class);
            assertThatThrownBy(() -> service.finish(CLIENT, locked)).isInstanceOf(SessionLogLockedException.class);
        }

        @Test
        void shouldRefuseTheSummary_whenTheSessionIsNotFinished() {
            SessionLogId session = withOneSet(CLIENT);

            assertThatThrownBy(() -> service.summary(CLIENT, session)).isInstanceOf(NotFinishedException.class);
        }

        @Test
        void shouldAnswerAsTheFirstTime_whenFinishedAgainAfterTheLock() {
            SessionLog log = new SessionLog(
                    SessionLogId.random(),
                    CLIENT,
                    TODAY.minusDays(3),
                    "Lower body",
                    NOW.minus(Duration.ofHours(60)),
                    NOW.minus(Duration.ofHours(59)),
                    List.of(new LoggedEntry(SQUAT.getId(), 1, 5, new BigDecimal("100.0"), 8, 120)),
                    List.of(new SetLog(
                            newId(), 0, 1, new SetValues(new BigDecimal("100.0"), 5, 8), new BigDecimal("100.0"), 5, 8,
                            NOW.minus(Duration.ofHours(59)))),
                    0);
            logs.save(log);

            SessionSummaryView view = service.finish(CLIENT, log.getId());

            assertThat(view.log().getFinishedAt()).contains(NOW.minus(Duration.ofHours(59)));
            assertThat(view.summary().setsLogged()).isEqualTo(1);
        }
    }

    @Nested
    class SaveNote {

        @Test
        void shouldStoreTheNote_onTheClientsFinishedSession() {
            SessionLogId session = service.start(CLIENT).log().getId();
            service.logSet(command(CLIENT, session, newId(), 0, 1, 5, 8));
            service.finish(CLIENT, session);

            service.saveNote(CLIENT, session, "Felt strong");

            assertThat(stored.get(session).getNote()).contains("Felt strong");
        }

        @Test
        void shouldAnswerNotFoundAndStoreNothing_whenTheSessionIsAnotherClientsOrUnknown() {
            SessionLogId session = service.start(CLIENT).log().getId();
            service.logSet(command(CLIENT, session, newId(), 0, 1, 5, 8));
            service.finish(CLIENT, session);

            assertThatThrownBy(() -> service.saveNote(OTHER_CLIENT, session, "Hi"))
                    .isInstanceOf(SessionLogNotFoundException.class);
            assertThatThrownBy(() -> service.saveNote(CLIENT, SessionLogId.random(), "Hi"))
                    .isInstanceOf(SessionLogNotFoundException.class);
            assertThat(stored.get(session).getNote()).isEmpty();
        }
    }
}
