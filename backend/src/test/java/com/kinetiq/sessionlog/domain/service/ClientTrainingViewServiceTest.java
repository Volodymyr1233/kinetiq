package com.kinetiq.sessionlog.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.ListExercisesQuery;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.input.DraftSummary;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanId;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.input.ViewQuizAnswersUseCase;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.model.QuizSubmissionId;
import com.kinetiq.sessionlog.domain.exception.SessionLogNotFoundException;
import com.kinetiq.sessionlog.domain.input.ClientOverview;
import com.kinetiq.sessionlog.domain.input.ClientSession;
import com.kinetiq.sessionlog.domain.input.ClientSessionLog;
import com.kinetiq.sessionlog.domain.model.LoggedEntry;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogHeader;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SessionState;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import com.kinetiq.sessionlog.domain.model.SetValues;
import com.kinetiq.sessionlog.domain.output.SessionLogRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClientTrainingViewServiceTest {

    private static final AccountId TRAINER = AccountId.random();
    private static final AccountId CLIENT = AccountId.random();
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDate WEEK_1_MONDAY = LocalDate.parse("2026-10-19");
    private static final LocalDate WEEK_3_MONDAY = WEEK_1_MONDAY.plusWeeks(2);
    private static final LocalDate WEEK_3_WEDNESDAY = WEEK_3_MONDAY.plusDays(2);
    // Wednesday of week 3, 11:00 in Warsaw
    private static final Instant WEDNESDAY = Instant.parse("2026-11-04T10:00:00Z");
    private static final Exercise SQUAT = exercise("Squat", Measure.REPS);
    private static final Exercise PLANK = exercise("Plank", Measure.SECONDS);
    private static final List<LoggedEntry> ENTRIES = List.of(
            new LoggedEntry(SQUAT.getId(), 3, 5, new BigDecimal("100.0"), 8, 120),
            new LoggedEntry(PLANK.getId(), 2, 45, null, 7, 60));
    private static final List<Session> SESSIONS = List.of(
            session("Upper body", DayOfWeek.MONDAY),
            session("Lower body", DayOfWeek.WEDNESDAY),
            session("Full body", DayOfWeek.FRIDAY));

    private PlanState planState = PlanState.PUBLISHED;
    private Plan plan = published(WEEK_1_MONDAY, WEEK_1_MONDAY, 8);
    private QuizSubmission answers = answers();
    private Instant now = WEDNESDAY;
    private final List<SessionLog> stored = new ArrayList<>();
    private final List<String> reads = new ArrayList<>();

    private static Exercise exercise(String name, Measure measure) {
        return new Exercise(
                ExerciseId.random(),
                TRAINER,
                new LocalizedText(name, name),
                Category.LEGS,
                new LocalizedText("", ""),
                measure,
                3,
                5,
                60,
                new LocalizedText("", ""));
    }

    private static Session session(String name, DayOfWeek day) {
        return new Session(
                name,
                day,
                List.of(
                        new ExerciseEntry(ExerciseId.random(), 3, 5, new BigDecimal("100.0"), 8, 120),
                        new ExerciseEntry(ExerciseId.random(), 2, 45, null, 7, 60)));
    }

    private static Plan published(LocalDate firstWeek, LocalDate publishedOn, int weeks) {
        return new Plan(PlanId.random(), CLIENT, Goal.GET_STRONGER, weeks, firstWeek, publishedOn, SESSIONS, 0, null);
    }

    private static QuizSubmission answers() {
        return new QuizSubmission(
                QuizSubmissionId.random(),
                CLIENT,
                Goal.BUILD_MUSCLE,
                Experience.YEARS_1_TO_2,
                Level.INTERMEDIATE,
                29,
                168,
                new BigDecimal("62.5"),
                Set.of(Like.WEIGHTS),
                3,
                null,
                Instant.parse("2026-10-15T10:00:00Z"));
    }

    // a session log of that date with one set logged for each RPE, finished when finishedAt is given
    private SessionLog log(LocalDate date, String name, String startedAt, String finishedAt, int... rpes) {
        return log(CLIENT, date, name, startedAt, finishedAt, rpes);
    }

    private SessionLog log(
            AccountId client, LocalDate date, String name, String startedAt, String finishedAt, int... rpes) {
        Instant start = Instant.parse(startedAt);
        SessionLog log = SessionLog.start(client, date, name, ENTRIES, start);
        for (int number = 1; number <= rpes.length; number++) {
            log.logSet(
                    new SetLogId(UUID.randomUUID()),
                    0,
                    number,
                    new SetValues(new BigDecimal("100.0"), 5, rpes[number - 1]),
                    start);
        }
        if (finishedAt != null) {
            log.finish(Instant.parse(finishedAt));
        }
        stored.add(log);
        return log;
    }

    private final ViewClientsUseCase clients = new ViewClientsUseCase() {
        @Override
        public List<ClientSummary> listClients(AccountId trainer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientSummary getClient(AccountId trainer, AccountId client) {
            if (!trainer.equals(TRAINER) || !client.equals(CLIENT)) {
                throw new ClientNotFoundException();
            }
            return new ClientSummary(CLIENT, "Ola", Goal.BUILD_MUSCLE, planState, ZONE);
        }
    };

    private final ViewQuizAnswersUseCase quiz = client -> {
        reads.add("quiz");
        return Optional.ofNullable(answers);
    };

    private final ViewOwnPlanUseCase planPort = new ViewOwnPlanUseCase() {
        @Override
        public OwnPlanView view(AccountId client, Integer week) {
            reads.add("plan");
            int current = plan.currentWeek(now.atZone(ZONE).toLocalDate()).orElseThrow();
            boolean finished = current > plan.getWeeks();
            OwnPlanView.Week shown = finished
                    ? null
                    : new OwnPlanView.Week(current, plan.getFirstWeek().orElseThrow().plusWeeks(current - 1L));
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

    private final SessionLogRepository logs = new SessionLogRepository() {
        @Override
        public List<SessionLog> findAllByClient(AccountId client) {
            reads.add("logs");
            return stored.stream()
                    .filter(log -> log.getClient().equals(client))
                    .sorted(Comparator.comparing(SessionLog::getDate).reversed())
                    .toList();
        }

        @Override
        public Optional<SessionLog> findByClientAndDate(AccountId client, LocalDate date) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<SessionLog> findByIdAndClient(SessionLogId id, AccountId client) {
            reads.add("log");
            return stored.stream()
                    .filter(log -> log.getId().equals(id) && log.getClient().equals(client))
                    .findFirst();
        }

        @Override
        public Optional<SessionLogId> sessionOfSetLog(SetLogId id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SessionLogHeader> headersBetween(AccountId client, LocalDate from, LocalDate to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SessionLog save(SessionLog log) {
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
            reads.add("library");
            return List.of(SQUAT, PLANK);
        }
    };

    private ClientTrainingViewService service() {
        return new ClientTrainingViewService(
                clients, quiz, planPort, exercises, logs, Clock.fixed(now, ZoneOffset.UTC));
    }

    private ClientOverview overview() {
        return service().overview(TRAINER, CLIENT);
    }

    private static ClientSession on(List<ClientSession> sessions, LocalDate date) {
        return sessions.stream()
                .filter(session -> session.getDate().equals(date))
                .findFirst()
                .orElseThrow();
    }

    private static List<ClientSession> week(ClientOverview overview) {
        return overview.getWeek().orElseThrow().sessions();
    }

    @Nested
    class Overview {

        @Test
        void shouldGiveEachSessionOfTheWeek_withItsStateAndFigures() {
            log(WEEK_3_MONDAY, "Upper body", "2026-11-02T17:00:00Z", "2026-11-02T17:50:00Z", 7, 8);

            ClientOverview overview = overview();

            assertThat(overview.getToday()).isEqualTo(WEEK_3_WEDNESDAY);
            assertThat(overview.getWeek()).hasValueSatisfying(week -> {
                assertThat(week.number()).isEqualTo(3);
                assertThat(week.startsOn()).isEqualTo(WEEK_3_MONDAY);
            });
            assertThat(week(overview))
                    .extracting(ClientSession::getName)
                    .containsExactly("Upper body", "Lower body", "Full body");
            ClientSession monday = on(week(overview), WEEK_3_MONDAY);
            assertThat(monday.getState()).contains(SessionState.DONE);
            assertThat(monday.isLocked()).isTrue();
            assertThat(monday.getSetsLogged()).isEqualTo(2);
            assertThat(monday.getSetsPlanned()).isEqualTo(5);
            assertThat(monday.getAverageRpe()).hasValueSatisfying(rpe -> assertThat(rpe).isEqualByComparingTo("7.5"));
            assertThat(monday.getSessionLogId()).isPresent();
            ClientSession today = on(week(overview), WEEK_3_WEDNESDAY);
            assertThat(today.getState()).contains(SessionState.TODAY);
            assertThat(today.isLocked()).isFalse();
            assertThat(today.getSetsLogged()).isZero();
            assertThat(today.getSetsPlanned()).isEqualTo(5);
            assertThat(today.getAverageRpe()).isEmpty();
            assertThat(today.getSessionLogId()).isEmpty();
            assertThat(on(week(overview), WEEK_3_MONDAY.plusDays(4)).getState()).contains(SessionState.PLANNED);
        }

        @Test
        void shouldTellDoneFromLocked_andInProgressFromMissed_byTheTwentyFourHours() {
            // Thursday 11:00: Wednesday's session ended 18 hours ago, Monday's unfinished one locked on Tuesday
            now = Instant.parse("2026-11-05T10:00:00Z");
            log(WEEK_3_WEDNESDAY, "Lower body", "2026-11-04T16:00:00Z", "2026-11-04T16:50:00Z", 8);
            log(WEEK_3_MONDAY, "Upper body", "2026-11-02T17:00:00Z", null, 9);

            ClientOverview overview = overview();

            ClientSession wednesday = on(week(overview), WEEK_3_WEDNESDAY);
            assertThat(wednesday.getState()).contains(SessionState.DONE);
            assertThat(wednesday.isLocked()).isFalse();
            ClientSession monday = on(week(overview), WEEK_3_MONDAY);
            assertThat(monday.getState()).contains(SessionState.MISSED);
            assertThat(monday.isLocked()).isTrue();
            assertThat(monday.getSetsLogged()).isEqualTo(1);
        }

        @Test
        void shouldGiveInProgress_toAStartedSessionWithinItsTwentyFourHours() {
            log(WEEK_3_WEDNESDAY, "Lower body", "2026-11-04T08:00:00Z", null, 8);

            ClientSession wednesday = on(week(overview()), WEEK_3_WEDNESDAY);

            assertThat(wednesday.getState()).contains(SessionState.IN_PROGRESS);
            assertThat(wednesday.isLocked()).isFalse();
        }

        @Test
        void shouldGiveNoState_toADayBeforeThePublishDay_andNotListIt() {
            plan = published(WEEK_3_MONDAY, WEEK_3_WEDNESDAY, 8);

            ClientOverview overview = overview();

            assertThat(on(week(overview), WEEK_3_MONDAY).getState()).isEmpty();
            assertThat(on(week(overview), WEEK_3_WEDNESDAY).getState()).contains(SessionState.TODAY);
            assertThat(overview.getRecentSessions()).isEmpty();
        }

        @Test
        void shouldListTheFourNewestSessions_withTheMissedOnesNeverStarted() {
            log(LocalDate.parse("2026-10-26"), "Upper body", "2026-10-26T17:00:00Z", "2026-10-26T17:50:00Z", 8);
            log(LocalDate.parse("2026-10-30"), "Full body", "2026-10-30T17:00:00Z", "2026-10-30T17:50:00Z", 8);
            log(WEEK_3_MONDAY, "Upper body", "2026-11-02T17:00:00Z", "2026-11-02T17:50:00Z", 8);
            // a Tuesday the plan does not have, as after the trainer moved the session
            log(LocalDate.parse("2026-11-03"), "Moved session", "2026-11-03T17:00:00Z", "2026-11-03T17:50:00Z", 8);

            List<ClientSession> recent = overview().getRecentSessions();

            // Wednesday 28 October was never started; today's session is not listed
            assertThat(recent)
                    .extracting(ClientSession::getDate)
                    .containsExactly(
                            LocalDate.parse("2026-11-03"),
                            WEEK_3_MONDAY,
                            LocalDate.parse("2026-10-30"),
                            LocalDate.parse("2026-10-28"));
            assertThat(recent.getFirst().getName()).isEqualTo("Moved session");
            ClientSession missed = recent.getLast();
            assertThat(missed.getName()).isEqualTo("Lower body");
            assertThat(missed.getState()).contains(SessionState.MISSED);
            assertThat(missed.isLocked()).isFalse();
            assertThat(missed.getSetsLogged()).isZero();
            assertThat(missed.getSetsPlanned()).isEqualTo(5);
            assertThat(missed.getAverageRpe()).isEmpty();
            assertThat(missed.getSessionLogId()).isEmpty();
        }

        @Test
        void shouldReadNoPlan_andHaveNoWeek_whenThePlanIsNotPublished() {
            planState = PlanState.DRAFT;

            ClientOverview overview = overview();

            assertThat(reads).doesNotContain("plan");
            assertThat(overview.getWeek()).isEmpty();
            assertThat(overview.getRecentSessions()).isEmpty();
            assertThat(overview.getQuizAnswers()).isPresent();
        }

        @Test
        void shouldHaveNoWeek_butKeepTheRecentSessions_whenThePlanHasFinished() {
            // weeks 1 to 4 from 5 October, so Wednesday 4 November is after the last week
            plan = published(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-05"), 4);

            ClientOverview overview = overview();

            assertThat(overview.getWeek()).isEmpty();
            assertThat(overview.getRecentSessions())
                    .extracting(ClientSession::getDate)
                    .containsExactly(
                            LocalDate.parse("2026-10-30"),
                            LocalDate.parse("2026-10-28"),
                            LocalDate.parse("2026-10-26"),
                            LocalDate.parse("2026-10-23"));
        }

        @Test
        void shouldLeaveTheQuizAnswersOut_beforeTheQuiz() {
            answers = null;

            assertThat(overview().getQuizAnswers()).isEmpty();
        }

        @Test
        void shouldTakeTodayInTheClientsTimeZone() {
            // 23:30 UTC on Tuesday is 00:30 on Wednesday in Warsaw
            now = Instant.parse("2026-11-03T23:30:00Z");

            ClientOverview overview = overview();

            assertThat(overview.getToday()).isEqualTo(WEEK_3_WEDNESDAY);
            assertThat(on(week(overview), WEEK_3_WEDNESDAY).getState()).contains(SessionState.TODAY);
        }

        @Test
        void shouldRefuseAnotherTrainersClient_andReadNothing() {
            assertThatThrownBy(() -> service().overview(AccountId.random(), CLIENT))
                    .isInstanceOf(ClientNotFoundException.class);
            assertThat(reads).isEmpty();
        }
    }

    @Nested
    class Sessions {

        @Test
        void shouldListEveryLogAndEveryMissedSession_newestFirst() {
            log(LocalDate.parse("2026-10-26"), "Upper body", "2026-10-26T17:00:00Z", "2026-10-26T17:50:00Z", 8);
            log(LocalDate.parse("2026-10-30"), "Full body", "2026-10-30T17:00:00Z", "2026-10-30T17:50:00Z", 8);
            log(WEEK_3_MONDAY, "Upper body", "2026-11-02T17:00:00Z", "2026-11-02T17:50:00Z", 8);
            log(LocalDate.parse("2026-11-03"), "Moved session", "2026-11-03T17:00:00Z", "2026-11-03T17:50:00Z", 8);

            List<ClientSession> sessions = service().sessions(TRAINER, CLIENT);

            // every planned day from the publish day on 19 October until yesterday, and the moved session
            assertThat(sessions)
                    .extracting(ClientSession::getDate, session -> session.getState().orElseThrow())
                    .containsExactly(
                            tuple(LocalDate.parse("2026-11-03"), SessionState.DONE),
                            tuple(WEEK_3_MONDAY, SessionState.DONE),
                            tuple(LocalDate.parse("2026-10-30"), SessionState.DONE),
                            tuple(LocalDate.parse("2026-10-28"), SessionState.MISSED),
                            tuple(LocalDate.parse("2026-10-26"), SessionState.DONE),
                            tuple(LocalDate.parse("2026-10-23"), SessionState.MISSED),
                            tuple(LocalDate.parse("2026-10-21"), SessionState.MISSED),
                            tuple(WEEK_1_MONDAY, SessionState.MISSED));
        }

        @Test
        void shouldRefuseAnotherTrainersClient_andReadNothing() {
            assertThatThrownBy(() -> service().sessions(AccountId.random(), CLIENT))
                    .isInstanceOf(ClientNotFoundException.class);
            assertThat(reads).isEmpty();
        }
    }

    @Nested
    class SessionLogOf {

        @Test
        void shouldAnswerTheClientsOwnLog_withItsSessionAndTheTrainersLibrary() {
            SessionLog monday = log(WEEK_3_MONDAY, "Upper body", "2026-11-02T17:00:00Z", "2026-11-02T17:50:00Z", 7, 8);

            ClientSessionLog read = service().sessionLog(TRAINER, CLIENT, monday.getId());

            assertThat(read.log()).isSameAs(monday);
            assertThat(read.session().getState()).contains(SessionState.DONE);
            assertThat(read.session().isLocked()).isTrue();
            assertThat(read.session().getSetsLogged()).isEqualTo(2);
            assertThat(read.session().getSessionLogId()).contains(monday.getId());
            assertThat(read.library()).containsEntry(SQUAT.getId(), SQUAT).containsEntry(PLANK.getId(), PLANK);
        }

        @Test
        void shouldNotFindAnotherClientsLog_underAnOwnClient() {
            SessionLog foreign = log(
                    AccountId.random(), WEEK_3_MONDAY, "Upper body", "2026-11-02T17:00:00Z", null, 8);

            assertThatThrownBy(() -> service().sessionLog(TRAINER, CLIENT, foreign.getId()))
                    .isInstanceOf(SessionLogNotFoundException.class);
            assertThat(reads).doesNotContain("library");
        }

        @Test
        void shouldNotFindAnUnknownLog() {
            assertThatThrownBy(() -> service().sessionLog(TRAINER, CLIENT, SessionLogId.random()))
                    .isInstanceOf(SessionLogNotFoundException.class);
        }

        @Test
        void shouldRefuseAnotherTrainersClient_andReadNothing() {
            SessionLog monday = log(WEEK_3_MONDAY, "Upper body", "2026-11-02T17:00:00Z", null, 8);

            assertThatThrownBy(() -> service().sessionLog(AccountId.random(), CLIENT, monday.getId()))
                    .isInstanceOf(ClientNotFoundException.class);
            assertThat(reads).isEmpty();
        }
    }
}
