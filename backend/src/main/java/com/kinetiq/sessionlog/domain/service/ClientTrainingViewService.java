package com.kinetiq.sessionlog.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.input.ViewQuizAnswersUseCase;
import com.kinetiq.readiness.domain.input.CheckInUseCase;
import com.kinetiq.readiness.domain.model.Cause;
import com.kinetiq.readiness.domain.model.CheckIn;
import com.kinetiq.readiness.domain.model.Planned;
import com.kinetiq.readiness.domain.model.Readiness;
import com.kinetiq.readiness.domain.model.Target;
import com.kinetiq.readiness.domain.model.TargetChoice;
import com.kinetiq.sessionlog.domain.exception.SessionLogNotFoundException;
import com.kinetiq.sessionlog.domain.input.ClientOverview;
import com.kinetiq.sessionlog.domain.input.ClientSession;
import com.kinetiq.sessionlog.domain.input.ClientSessionLog;
import com.kinetiq.sessionlog.domain.input.ViewClientTrainingUseCase;
import com.kinetiq.sessionlog.domain.model.LoggedEntry;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogHeader;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SessionState;
import com.kinetiq.sessionlog.domain.output.SessionLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClientTrainingViewService implements ViewClientTrainingUseCase {

    private static final int RECENT_SESSIONS = 4;
    private static final int READINESS_DAYS = 14;

    private final ViewClientsUseCase clients;
    private final ViewQuizAnswersUseCase quiz;
    private final ViewOwnPlanUseCase plan;
    private final ListExercisesUseCase exercises;
    private final SessionLogRepository logs;
    private final CheckInUseCase checkIns;
    private final Clock clock;

    @Override
    public ClientOverview overview(AccountId trainer, AccountId client) {
        ClientSummary summary = clients.getClient(trainer, client);
        Instant now = clock.instant();
        LocalDate today = now.atZone(summary.getTimeZone()).toLocalDate();
        Map<LocalDate, SessionLog> byDate = byDate(client);
        Optional<OwnPlanView> view = published(summary, client);
        Map<LocalDate, CheckIn> given = checkIns(client, today);
        return new ClientOverview(
                today,
                view.filter(published -> published.week() != null)
                        .map(published -> week(published, byDate, today, now))
                        .orElse(null),
                newestFirst(byDate, view.map(v -> neverStarted(v, byDate, today)).orElse(List.of()), now).stream()
                        .limit(RECENT_SESSIONS)
                        .toList(),
                quiz.answers(client).orElse(null),
                readiness(given, today),
                recoveryDays(trainer, client, given, today, now, byDate, view));
    }

    @Override
    public List<ClientSession> sessions(AccountId trainer, AccountId client) {
        ClientSummary summary = clients.getClient(trainer, client);
        Instant now = clock.instant();
        LocalDate today = now.atZone(summary.getTimeZone()).toLocalDate();
        Map<LocalDate, SessionLog> byDate = byDate(client);
        return newestFirst(
                byDate,
                published(summary, client).map(v -> neverStarted(v, byDate, today)).orElse(List.of()),
                now);
    }

    @Override
    public ClientSessionLog sessionLog(AccountId trainer, AccountId client, SessionLogId id) {
        clients.getClient(trainer, client);
        SessionLog log = logs.findByIdAndClient(id, client).orElseThrow(SessionLogNotFoundException::new);
        Map<ExerciseId, Exercise> library = library(trainer);
        return new ClientSessionLog(
                log,
                started(log, clock.instant()),
                library,
                checkIns.adjustmentOn(client, log.getDate(), planned(log, library))
                        .orElse(null));
    }

    private static List<ClientOverview.ReadinessDay> readiness(Map<LocalDate, CheckIn> byDate, LocalDate today) {
        return Stream.iterate(today.minusDays(READINESS_DAYS - 1L), date -> date.plusDays(1))
                .limit(READINESS_DAYS)
                .map(date -> new ClientOverview.ReadinessDay(
                        date,
                        Optional.ofNullable(byDate.get(date))
                                .map(CheckIn::readiness)
                                .orElse(null)))
                .toList();
    }

    private List<ClientOverview.RecoveryDay> recoveryDays(
            AccountId trainer,
            AccountId client,
            Map<LocalDate, CheckIn> given,
            LocalDate today,
            Instant now,
            Map<LocalDate, SessionLog> byDate,
            Optional<OwnPlanView> view) {
        return given.values().stream()
                .filter(checkIn -> checkIn.readiness().band() == Readiness.Band.RECOVERY_DAY)
                .sorted(Comparator.comparing(CheckIn::getDate).reversed())
                .map(checkIn ->
                        recoveryDay(trainer, client, checkIn, today, now, byDate.get(checkIn.getDate()), view))
                .toList();
    }

    private ClientOverview.RecoveryDay recoveryDay(
            AccountId trainer,
            AccountId client,
            CheckIn checkIn,
            LocalDate today,
            Instant now,
            SessionLog log,
            Optional<OwnPlanView> view) {
        LocalDate date = checkIn.getDate();
        ClientSession session = null;
        List<Planned> planned = List.of();
        if (log != null) {
            session = started(log, now);
            planned = planned(log, library(trainer));
        } else {
            Optional<OwnPlanView> within = view.filter(published -> withinPlan(published.plan(), date));
            Optional<Session> onDay = within.flatMap(published -> published.plan().getSessions().stream()
                    .filter(candidate -> candidate.day() == date.getDayOfWeek())
                    .findFirst());
            if (onDay.isPresent()) {
                LocalDate publishedOn = within.orElseThrow().plan().getPublishedOn().orElseThrow();
                session = planned(
                        onDay.get(),
                        date,
                        SessionState.of(date, today, publishedOn, Optional.empty(), now).orElse(null));
                planned = planned(onDay.get(), within.orElseThrow().library());
            }
        }
        TargetChoice targets = planned.isEmpty()
                ? null
                : checkIns.adjustmentOn(client, date, planned)
                        .map(adjustment -> adjustment.inUse() ? TargetChoice.LIGHTER : TargetChoice.ORIGINAL)
                        .orElse(null);
        return new ClientOverview.RecoveryDay(
                date, checkIn.readiness().score(), Cause.of(checkIn.getAnswers()), targets, session);
    }

    // check-ins are kept by the client's local date, so the last 14 of them end on today
    private Map<LocalDate, CheckIn> checkIns(AccountId client, LocalDate today) {
        return checkIns.between(client, today.minusDays(READINESS_DAYS - 1L), today).stream()
                .collect(Collectors.toMap(CheckIn::getDate, Function.identity()));
    }

    private static boolean withinPlan(Plan plan, LocalDate date) {
        LocalDate publishedOn = plan.getPublishedOn().orElseThrow();
        LocalDate end = plan.getFirstWeek().orElseThrow().plusWeeks(plan.getWeeks());
        return !date.isBefore(publishedOn) && date.isBefore(end);
    }

    private Map<ExerciseId, Exercise> library(AccountId trainer) {
        return exercises.library(trainer).stream().collect(Collectors.toMap(Exercise::getId, Function.identity()));
    }

    // a started session's own copy, which a later plan edit does not change
    private static List<Planned> planned(SessionLog log, Map<ExerciseId, Exercise> library) {
        return log.getEntries().stream()
                .map(entry -> new Planned(target(entry), library.get(entry.exercise()).getMeasure()))
                .toList();
    }

    private static List<Planned> planned(Session session, Map<ExerciseId, Exercise> library) {
        return session.entries().stream()
                .map(entry -> new Planned(
                        new Target(entry.loadKg(), entry.amount(), entry.rpe()),
                        library.get(entry.exercise()).getMeasure()))
                .toList();
    }

    private static Target target(LoggedEntry entry) {
        return new Target(entry.loadKg(), entry.amount(), entry.rpe());
    }

    // a client has one session log a day at most
    private Map<LocalDate, SessionLog> byDate(AccountId client) {
        return logs.findAllByClient(client).stream()
                .collect(Collectors.toMap(SessionLog::getDate, Function.identity()));
    }

    // a draft has no week, and asking for one would only be refused as no plan
    private Optional<OwnPlanView> published(ClientSummary summary, AccountId client) {
        return summary.getPlanState() == PlanState.PUBLISHED
                ? Optional.of(plan.view(client, null))
                : Optional.empty();
    }

    private static List<ClientSession> newestFirst(
            Map<LocalDate, SessionLog> byDate, List<ClientSession> neverStarted, Instant now) {
        return Stream.concat(byDate.values().stream().map(log -> started(log, now)), neverStarted.stream())
                .sorted(Comparator.comparing(ClientSession::getDate).reversed())
                .toList();
    }

    private static ClientOverview.Week week(
            OwnPlanView view, Map<LocalDate, SessionLog> byDate, LocalDate today, Instant now) {
        LocalDate monday = view.week().startsOn();
        LocalDate publishedOn = view.plan().getPublishedOn().orElseThrow();
        return new ClientOverview.Week(
                view.week().number(),
                monday,
                view.plan().getSessions().stream()
                        .map(session -> {
                            LocalDate date = monday.plusDays(session.day().getValue() - 1L);
                            SessionLog log = byDate.get(date);
                            return log != null
                                    ? started(log, now)
                                    : planned(
                                            session,
                                            date,
                                            SessionState.of(date, today, publishedOn, Optional.empty(), now)
                                                    .orElse(null));
                        })
                        .toList());
    }

    // the planned days from the publish day until yesterday that have no session log
    private static List<ClientSession> neverStarted(
            OwnPlanView view, Map<LocalDate, SessionLog> byDate, LocalDate today) {
        Plan published = view.plan();
        LocalDate firstWeek = published.getFirstWeek().orElseThrow();
        LocalDate publishedOn = published.getPublishedOn().orElseThrow();
        List<ClientSession> missed = new ArrayList<>();
        for (int number = 1; number <= Math.min(view.currentWeek(), published.getWeeks()); number++) {
            LocalDate monday = firstWeek.plusWeeks(number - 1L);
            for (Session session : published.getSessions()) {
                LocalDate date = monday.plusDays(session.day().getValue() - 1L);
                if (!date.isBefore(publishedOn) && date.isBefore(today) && !byDate.containsKey(date)) {
                    missed.add(planned(session, date, SessionState.MISSED));
                }
            }
        }
        return missed;
    }

    private static ClientSession started(SessionLog log, Instant now) {
        SessionLogHeader header = new SessionLogHeader(
                log.getId(), log.getDate(), log.getStartedAt(), log.getFinishedAt().orElse(null));
        // with a session log the state follows from the log alone, whatever the day
        SessionState state = SessionState.of(log.getDate(), log.getDate(), log.getDate(), Optional.of(header), now)
                .orElseThrow();
        return new ClientSession(
                log.getDate(),
                log.getName(),
                state,
                !header.open(now),
                log.getSetLogs().size(),
                log.setsPlanned(),
                log.averageRpe().orElse(null),
                log.getId());
    }

    private static ClientSession planned(Session session, LocalDate date, SessionState state) {
        return new ClientSession(
                date,
                session.name(),
                state,
                false,
                0,
                session.entries().stream().mapToInt(ExerciseEntry::sets).sum(),
                null,
                null);
    }
}
