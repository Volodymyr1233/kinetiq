package com.kinetiq.sessionlog.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.sessionlog.domain.exception.NoSessionTodayException;
import com.kinetiq.sessionlog.domain.exception.SessionLogNotFoundException;
import com.kinetiq.sessionlog.domain.input.LogSessionUseCase;
import com.kinetiq.sessionlog.domain.input.LogSetCommand;
import com.kinetiq.sessionlog.domain.input.SessionSummaryView;
import com.kinetiq.sessionlog.domain.input.StartedSession;
import com.kinetiq.sessionlog.domain.model.LoggedEntry;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLog;
import com.kinetiq.sessionlog.domain.model.SetValues;
import com.kinetiq.sessionlog.domain.output.SessionLogRepository;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SessionLogService implements LogSessionUseCase {

    private final ViewOwnPlanUseCase plan;
    private final CurrentAccountUseCase accounts;
    private final ListExercisesUseCase exercises;
    private final SessionLogRepository logs;
    private final Clock clock;

    // ponytail: two starts at the same moment both pass the lookup and the second is refused by the database as a 500;
    // retry on that constraint if it ever shows
    @Override
    public StartedSession start(AccountId client) {
        TodayView today = plan.today(client);
        if (today.state() != TodayView.State.SESSION) {
            throw new NoSessionTodayException();
        }
        Optional<SessionLog> existing = logs.findByClientAndDate(client, today.date());
        if (existing.isPresent()) {
            return new StartedSession(existing.get(), library(client), false);
        }
        Session session = today.session();
        SessionLog started = SessionLog.start(
                client,
                today.date(),
                session.name(),
                session.entries().stream()
                        .map(entry -> new LoggedEntry(
                                entry.exercise(),
                                entry.sets(),
                                entry.amount(),
                                entry.loadKg(),
                                entry.rpe(),
                                entry.restSeconds()))
                        .toList(),
                clock.instant());
        return new StartedSession(logs.save(started), library(client), true);
    }

    @Override
    public StartedSession find(AccountId client, SessionLogId id) {
        return new StartedSession(ownLog(client, id), library(client), false);
    }

    @Override
    public SetLog logSet(LogSetCommand command) {
        SessionLog log = logs.findByIdAndClient(command.session(), command.client())
                .orElseThrow(SessionLogNotFoundException::new);
        // an id of another session is never taken over, and does not show that it exists
        logs.sessionOfSetLog(command.setLogId()).filter(id -> !id.equals(log.getId())).ifPresent(other -> {
            throw new SessionLogNotFoundException();
        });
        SetLog logged = log.logSet(
                command.setLogId(),
                command.entryPosition(),
                command.setNumber(),
                new SetValues(command.loadKg(), command.amount(), command.rpe()),
                clock.instant());
        logs.save(log);
        return logged;
    }

    @Override
    public SessionSummaryView finish(AccountId client, SessionLogId id) {
        SessionLog log = ownLog(client, id);
        log.finish(clock.instant());
        return view(logs.save(log), client);
    }

    @Override
    public SessionSummaryView summary(AccountId client, SessionLogId id) {
        return view(ownLog(client, id), client);
    }

    @Override
    public void saveNote(AccountId client, SessionLogId id, String text) {
        SessionLog log = ownLog(client, id);
        log.writeNote(text, clock.instant());
        logs.save(log);
    }

    private SessionLog ownLog(AccountId client, SessionLogId id) {
        return logs.findByIdAndClient(id, client).orElseThrow(SessionLogNotFoundException::new);
    }

    private SessionSummaryView view(SessionLog log, AccountId client) {
        Map<ExerciseId, Measure> measures = library(client).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getMeasure()));
        return new SessionSummaryView(log, log.summary(measures));
    }

    private Map<ExerciseId, Exercise> library(AccountId client) {
        AccountId trainer = accounts.find(client).getTrainerId().orElseThrow(PlanNotFoundException::new);
        return exercises.library(trainer).stream().collect(Collectors.toMap(Exercise::getId, Function.identity()));
    }
}
