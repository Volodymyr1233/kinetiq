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
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.exception.PlanWeekOutOfRangeException;
import com.kinetiq.plan.domain.input.DraftSummary;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanId;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.sessionlog.domain.input.SessionStates;
import com.kinetiq.sessionlog.domain.model.SessionLogHeader;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SessionState;
import com.kinetiq.sessionlog.domain.output.SessionLogRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SessionStateServiceTest {

    private static final AccountId TRAINER = AccountId.random();
    private static final AccountId CLIENT = AccountId.random();
    private static final LocalDate WEEK_3_MONDAY = LocalDate.parse("2026-11-02");
    private static final LocalDate WEEK_1_MONDAY = WEEK_3_MONDAY.minusWeeks(2);
    // Saturday of week 3
    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");

    private static final List<Session> SESSIONS = List.of(
            session(DayOfWeek.MONDAY),
            session(DayOfWeek.TUESDAY),
            session(DayOfWeek.WEDNESDAY),
            session(DayOfWeek.SATURDAY),
            session(DayOfWeek.SUNDAY));

    private Plan plan = published(WEEK_1_MONDAY, WEEK_1_MONDAY, 8);
    private ZoneId zone = ZoneId.of("Europe/Warsaw");
    private Instant now = NOW;
    private final List<SessionLogHeader> headers = new ArrayList<>();

    private static Session session(DayOfWeek day) {
        return new Session(
                "S " + day,
                day,
                List.of(new ExerciseEntry(ExerciseId.random(), 3, 5, new BigDecimal("100.0"), 8, 120)));
    }

    private static Plan published(LocalDate firstWeek, LocalDate publishedOn, int weeks) {
        return new Plan(PlanId.random(), CLIENT, Goal.GET_STRONGER, weeks, firstWeek, publishedOn, SESSIONS, 0, null);
    }

    private static SessionLogHeader header(LocalDate date, Instant startedAt, Instant finishedAt) {
        return new SessionLogHeader(SessionLogId.random(), date, startedAt, finishedAt);
    }

    private final ViewOwnPlanUseCase planPort = new ViewOwnPlanUseCase() {
        @Override
        public OwnPlanView view(AccountId client, Integer week) {
            if (plan == null || !plan.isPublished()) {
                throw new PlanNotFoundException();
            }
            LocalDate today = now.atZone(zone).toLocalDate();
            int current = plan.currentWeek(today).orElseThrow();
            boolean finished = current > plan.getWeeks();
            if (finished && week != null || week != null && (week < 1 || week > plan.getWeeks())) {
                throw new PlanWeekOutOfRangeException();
            }
            OwnPlanView.Week shown = finished
                    ? null
                    : new OwnPlanView.Week(
                            week == null ? current : week,
                            plan.getFirstWeek().orElseThrow().plusWeeks((week == null ? current : week) - 1L));
            return new OwnPlanView(plan, shown, current, finished, true, Map.of());
        }

        @Override
        public TodayView today(AccountId client) {
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
                    zone,
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

    private final SessionLogRepository logs = new SessionLogRepositoryStub(headers);

    private SessionStates states(Integer week) {
        return new SessionStateService(planPort, accounts, logs, Clock.fixed(now, ZoneOffset.UTC)).states(CLIENT, week);
    }

    private static Optional<SessionState> stateOf(SessionStates states, DayOfWeek day) {
        return states.sessions().stream()
                .filter(entry -> entry.day() == day)
                .findFirst()
                .orElseThrow()
                .state();
    }

    @Test
    void shouldGiveTheStateAndLogIdOfEverySessionOfTheWeek() {
        SessionLogHeader done = header(
                WEEK_3_MONDAY, Instant.parse("2026-11-02T08:00:00Z"), Instant.parse("2026-11-02T09:00:00Z"));
        // started Tuesday 18:00, locks Wednesday 18:00, which is long past
        SessionLogHeader lockedUnfinished = header(WEEK_3_MONDAY.plusDays(1), Instant.parse("2026-11-03T18:00:00Z"), null);
        // started Friday evening
        SessionLogHeader inProgress = header(WEEK_3_MONDAY.plusDays(5), Instant.parse("2026-11-07T08:00:00Z"), null);
        headers.addAll(List.of(done, lockedUnfinished, inProgress));

        SessionStates states = states(null);

        assertThat(states.today()).isEqualTo(LocalDate.parse("2026-11-07"));
        assertThat(states.sessions()).extracting(SessionStates.Entry::day)
                .containsExactly(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY);
        assertThat(stateOf(states, DayOfWeek.MONDAY)).contains(SessionState.DONE);
        assertThat(stateOf(states, DayOfWeek.TUESDAY)).contains(SessionState.MISSED);
        assertThat(stateOf(states, DayOfWeek.WEDNESDAY)).contains(SessionState.MISSED);
        assertThat(stateOf(states, DayOfWeek.SATURDAY)).contains(SessionState.IN_PROGRESS);
        assertThat(stateOf(states, DayOfWeek.SUNDAY)).contains(SessionState.PLANNED);
        assertThat(states.sessions().getFirst().sessionLogId()).contains(done.id());
        assertThat(states.sessions().get(2).sessionLogId()).isEmpty();
    }

    @Test
    void shouldGiveToday_whenTodaysSessionWasNotStarted() {
        now = Instant.parse("2026-11-07T10:00:00Z");

        assertThat(stateOf(states(null), DayOfWeek.SATURDAY)).contains(SessionState.TODAY);
    }

    @Test
    void shouldGivePastWeeksAsMissedOrDone_andFutureWeeksAsPlanned() {
        SessionLogHeader done = header(
                WEEK_1_MONDAY, Instant.parse("2026-10-19T08:00:00Z"), Instant.parse("2026-10-19T09:00:00Z"));
        headers.add(done);

        SessionStates past = states(1);
        SessionStates future = states(5);

        assertThat(stateOf(past, DayOfWeek.MONDAY)).contains(SessionState.DONE);
        assertThat(stateOf(past, DayOfWeek.TUESDAY)).contains(SessionState.MISSED);
        assertThat(future.sessions()).allSatisfy(entry -> assertThat(entry.state()).contains(SessionState.PLANNED));
        assertThat(future.sessions().getFirst().date()).isEqualTo(WEEK_1_MONDAY.plusWeeks(4));
    }

    @Test
    void shouldShowNoState_forDaysBeforeThePlanWasPublished() {
        plan = published(WEEK_3_MONDAY, WEEK_3_MONDAY.plusDays(2), 8);

        SessionStates states = states(null);

        assertThat(stateOf(states, DayOfWeek.MONDAY)).isEmpty();
        assertThat(stateOf(states, DayOfWeek.TUESDAY)).isEmpty();
        assertThat(stateOf(states, DayOfWeek.WEDNESDAY)).contains(SessionState.MISSED);
    }

    @Test
    void shouldTakeTodayFromTheClientsTimeZone() {
        // 23:30 on Saturday in Warsaw is still Saturday in New York, where it is 17:30
        now = Instant.parse("2026-11-07T22:30:00Z");
        zone = ZoneId.of("America/New_York");

        SessionStates states = states(null);

        assertThat(states.today()).isEqualTo(LocalDate.parse("2026-11-07"));
        assertThat(stateOf(states, DayOfWeek.SATURDAY)).contains(SessionState.TODAY);
    }

    @Test
    void shouldNotListALogOnADateTheWeekHasNoSessionFor() {
        headers.add(header(WEEK_3_MONDAY.plusDays(3), Instant.parse("2026-11-05T08:00:00Z"), null));

        assertThat(states(null).sessions()).hasSize(SESSIONS.size());
        assertThat(states(null).sessions()).allSatisfy(entry -> assertThat(entry.sessionLogId())
                .isNotEqualTo(Optional.of(headers.getFirst().id())));
    }

    @Test
    void shouldGiveNoSessions_whenThePlanHasFinishedAndNoWeekIsAsked() {
        plan = published(WEEK_1_MONDAY.minusWeeks(20), WEEK_1_MONDAY.minusWeeks(20), 8);

        assertThat(states(null).sessions()).isEmpty();
        assertThatThrownBy(() -> states(1)).isInstanceOf(PlanWeekOutOfRangeException.class);
    }

    @Test
    void shouldAnswerNotFound_forNoPlanAndForADraft() {
        plan = null;
        assertThatThrownBy(() -> states(null)).isInstanceOf(PlanNotFoundException.class);

        plan = Plan.draft(CLIENT, Goal.GET_STRONGER, 8, SESSIONS);
        assertThatThrownBy(() -> states(null)).isInstanceOf(PlanNotFoundException.class);
    }
}
