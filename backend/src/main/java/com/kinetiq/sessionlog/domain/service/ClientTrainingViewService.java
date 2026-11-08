package com.kinetiq.sessionlog.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.input.ViewQuizAnswersUseCase;
import com.kinetiq.sessionlog.domain.input.ClientOverview;
import com.kinetiq.sessionlog.domain.input.ClientSession;
import com.kinetiq.sessionlog.domain.input.ViewClientTrainingUseCase;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogHeader;
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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClientTrainingViewService implements ViewClientTrainingUseCase {

    private static final int RECENT_SESSIONS = 4;

    private final ViewClientsUseCase clients;
    private final ViewQuizAnswersUseCase quiz;
    private final ViewOwnPlanUseCase plan;
    private final SessionLogRepository logs;
    private final Clock clock;

    @Override
    public ClientOverview overview(AccountId trainer, AccountId client) {
        ClientSummary summary = clients.getClient(trainer, client);
        Instant now = clock.instant();
        LocalDate today = now.atZone(summary.getTimeZone()).toLocalDate();
        // a client has one session log a day at most
        Map<LocalDate, SessionLog> byDate = logs.findAllByClient(client).stream()
                .collect(Collectors.toMap(SessionLog::getDate, Function.identity()));
        List<ClientSession> sessions = new ArrayList<>(
                byDate.values().stream().map(log -> started(log, now)).toList());
        ClientOverview.Week week = null;
        // a draft has no week, and asking for one would only be refused as no plan
        if (summary.getPlanState() == PlanState.PUBLISHED) {
            OwnPlanView view = plan.view(client, null);
            sessions.addAll(neverStarted(view, byDate, today));
            week = view.week() == null ? null : week(view, byDate, today, now);
        }
        sessions.sort(Comparator.comparing(ClientSession::getDate).reversed());
        return new ClientOverview(
                today,
                week,
                sessions.stream().limit(RECENT_SESSIONS).toList(),
                quiz.answers(client).orElse(null));
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
