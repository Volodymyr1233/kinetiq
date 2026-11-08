package com.kinetiq.sessionlog.infrastructure.input;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.sessionlog.domain.input.SessionStates;
import com.kinetiq.sessionlog.domain.input.SessionSummaryView;
import com.kinetiq.sessionlog.domain.input.StartedSession;
import com.kinetiq.sessionlog.domain.model.LoggedEntry;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SessionSummary;
import com.kinetiq.sessionlog.domain.model.SetLog;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
class SessionLogWebMapper {

    SessionLogResponse toResponse(StartedSession started, Locale locale) {
        SessionLog log = started.log();
        List<LoggedEntry> entries = log.getEntries();
        return new SessionLogResponse(
                log.getId().value(),
                log.getDate(),
                log.getName(),
                log.getStartedAt(),
                log.getFinishedAt().orElse(null),
                IntStream.range(0, entries.size())
                        .mapToObj(position -> toResponse(
                                position,
                                entries.get(position),
                                started.library().get(entries.get(position).exercise()),
                                log,
                                locale))
                        .toList());
    }

    private SessionLogResponse.LoggedEntryResponse toResponse(
            int position, LoggedEntry entry, Exercise exercise, SessionLog log, Locale locale) {
        return new SessionLogResponse.LoggedEntryResponse(
                position,
                entry.exercise().value(),
                exercise.getName().in(locale),
                exercise.getMeasure(),
                entry.restSeconds(),
                IntStream.rangeClosed(1, entry.sets())
                        .mapToObj(number -> new SessionLogResponse.PlannedSetResponse(
                                number,
                                new SessionLogResponse.TargetResponse(entry.loadKg(), entry.amount(), entry.rpe()),
                                loggedAt(log, position, number)
                                        .map(this::toLogged)
                                        .orElse(null)))
                        .toList());
    }

    private Optional<SetLog> loggedAt(SessionLog log, int position, int number) {
        return log.getSetLogs().stream()
                .filter(set -> set.entryPosition() == position && set.setNumber() == number)
                .findFirst();
    }

    private SessionLogResponse.LoggedSetResponse toLogged(SetLog set) {
        return new SessionLogResponse.LoggedSetResponse(
                set.id().value(),
                set.values().loadKg(),
                set.values().amount(),
                set.values().rpe(),
                set.loggedAt());
    }

    SessionSummaryResponse toResponse(SessionSummaryView view) {
        SessionLog log = view.log();
        SessionSummary summary = view.summary();
        return new SessionSummaryResponse(
                log.getId().value(),
                log.getName(),
                log.getDate(),
                log.getStartedAt(),
                summary.setsLogged(),
                summary.setsPlanned(),
                summary.volumeKg().orElse(null),
                summary.averageRpe(),
                summary.minutes(),
                log.getNote().orElse(null));
    }

    SessionStatesResponse toResponse(SessionStates states) {
        return new SessionStatesResponse(
                states.today(),
                states.sessions().stream()
                        .map(entry -> new SessionStatesResponse.SessionStateResponse(
                                entry.day(),
                                entry.date(),
                                entry.state().orElse(null),
                                entry.sessionLogId().map(SessionLogId::value).orElse(null),
                                entry.startedAt().orElse(null)))
                        .toList());
    }

    SetLogResponse toResponse(SetLog set) {
        return new SetLogResponse(
                set.id().value(),
                set.entryPosition(),
                set.setNumber(),
                set.values().loadKg(),
                set.values().amount(),
                set.values().rpe(),
                set.loggedAt());
    }
}
