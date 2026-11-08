package com.kinetiq.sessionlog.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.Plan;
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
                                    log.map(SessionLogHeader::startedAt));
                        })
                        .toList());
    }
}
