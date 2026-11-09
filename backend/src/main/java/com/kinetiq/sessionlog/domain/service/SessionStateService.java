package com.kinetiq.sessionlog.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.readiness.domain.input.CheckInUseCase;
import com.kinetiq.readiness.domain.model.Adjustment;
import com.kinetiq.readiness.domain.model.Planned;
import com.kinetiq.readiness.domain.model.Target;
import com.kinetiq.sessionlog.domain.input.SessionStates;
import com.kinetiq.sessionlog.domain.input.ViewSessionStatesUseCase;
import com.kinetiq.sessionlog.domain.model.SessionLogHeader;
import com.kinetiq.sessionlog.domain.model.SessionState;
import com.kinetiq.sessionlog.domain.output.SessionLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SessionStateService implements ViewSessionStatesUseCase {

    private static final int DAYS_PER_WEEK = 7;

    private final ViewOwnPlanUseCase plan;
    private final CurrentAccountUseCase accounts;
    private final SessionLogRepository logs;
    private final ListExercisesUseCase exercises;
    private final CheckInUseCase checkIns;
    private final Clock clock;

    @Override
    public SessionStates states(AccountId client, Integer week) {
        OwnPlanView view = plan.view(client, week);
        Instant now = clock.instant();
        LocalDate today = now.atZone(accounts.find(client).getTimeZone()).toLocalDate();
        if (view.week() == null) {
            return new SessionStates(today, List.of());
        }
        Plan published = view.plan();
        LocalDate monday = view.week().startsOn();
        // a session log is matched to its planned session by date: the log holds a copy, not a link
        Map<LocalDate, SessionLogHeader> byDate = logs.headersBetween(client, monday, monday.plusDays(DAYS_PER_WEEK - 1L))
                .stream()
                .collect(Collectors.toMap(SessionLogHeader::date, Function.identity()));
        LocalDate publishedOn = published.getPublishedOn().orElseThrow();
        Map<ExerciseId, Measure> measures = measures(client);
        return new SessionStates(
                today,
                published.getSessions().stream()
                        .map(session -> {
                            LocalDate date = monday.plusDays(session.day().getValue() - 1L);
                            Optional<SessionLogHeader> log = Optional.ofNullable(byDate.get(date));
                            return new SessionStates.Entry(
                                    session.day(),
                                    date,
                                    SessionState.of(date, today, publishedOn, log, now),
                                    log.map(SessionLogHeader::id),
                                    log.map(SessionLogHeader::startedAt),
                                    // a check-in exists only for a day that has come
                                    !date.isAfter(today) && adjusted(client, date, session, measures));
                        })
                        .toList());
    }

    // ponytail: the plan's targets, not a started session's copy; they differ only after a plan revision, which almost
    // never changes whether a target gets lighter. Read the log's copy if a marked day ever disagrees
    private boolean adjusted(AccountId client, LocalDate date, Session session, Map<ExerciseId, Measure> measures) {
        List<Planned> planned = session.entries().stream()
                .map(entry -> new Planned(
                        new Target(entry.loadKg(), entry.amount(), entry.rpe()), measures.get(entry.exercise())))
                .toList();
        return checkIns.adjustmentOn(client, date, planned).map(Adjustment::inUse).orElse(false);
    }

    private Map<ExerciseId, Measure> measures(AccountId client) {
        AccountId trainer = accounts.find(client).getTrainerId().orElseThrow(PlanNotFoundException::new);
        return exercises.library(trainer).stream().collect(Collectors.toMap(Exercise::getId, Exercise::getMeasure));
    }
}
