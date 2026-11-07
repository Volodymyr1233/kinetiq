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
import com.kinetiq.sessionlog.domain.exception.NoSessionTodayException;
import com.kinetiq.sessionlog.domain.exception.SessionLogNotFoundException;
import com.kinetiq.sessionlog.domain.exception.SetValuesInvalidException;
import com.kinetiq.sessionlog.domain.input.LogSetCommand;
import com.kinetiq.sessionlog.domain.input.StartedSession;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLog;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import com.kinetiq.sessionlog.domain.output.SessionLogRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

    private TodayView today = sessionDay("Lower body");
    private RuntimeException planFailure;
    private final Map<SessionLogId, SessionLog> stored = new LinkedHashMap<>();

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
            return List.of(SQUAT);
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
        public SessionLog save(SessionLog log) {
            stored.put(log.getId(), log);
            return log;
        }
    };

    private final SessionLogService service =
            new SessionLogService(plan, accounts, exercises, logs, Clock.fixed(NOW, ZoneOffset.UTC));

    private static LogSetCommand command(
            AccountId client, SessionLogId session, SetLogId id, int position, int number, int amount, int rpe) {
        return new LogSetCommand(client, session, id, position, number, new BigDecimal("100.0"), amount, rpe);
    }

    private static SetLogId newId() {
        return new SetLogId(UUID.randomUUID());
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
    }
}
