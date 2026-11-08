package com.kinetiq.sessionlog.infrastructure.input;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.sessionlog.domain.input.ClientOverview;
import com.kinetiq.sessionlog.domain.input.ClientSession;
import com.kinetiq.sessionlog.domain.input.ClientSessionLog;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
class SessionLogWebMapper {

    SessionLogResponse toResponse(StartedSession started, Locale locale) {
        SessionLog log = started.log();
        return new SessionLogResponse(
                log.getId().value(),
                log.getDate(),
                log.getName(),
                log.getStartedAt(),
                log.getFinishedAt().orElse(null),
                entries(log, started.library(), locale));
    }

    ClientSessionLogResponse toResponse(ClientSessionLog read, Locale locale) {
        SessionLog log = read.log();
        ClientSession session = read.session();
        return new ClientSessionLogResponse(
                log.getId().value(),
                session.getDate(),
                session.getDate().getDayOfWeek(),
                session.getName(),
                session.getState().orElseThrow(),
                session.isLocked(),
                session.getSetsLogged(),
                session.getSetsPlanned(),
                session.getAverageRpe().orElse(null),
                log.getStartedAt(),
                log.getFinishedAt().orElse(null),
                log.minutes().orElse(null),
                log.getNote().orElse(null),
                entries(log, read.library(), locale));
    }

    ClientSessionsResponse toResponse(List<ClientSession> sessions) {
        return new ClientSessionsResponse(toResponses(sessions));
    }

    private List<SessionLogResponse.LoggedEntryResponse> entries(
            SessionLog log, Map<ExerciseId, Exercise> library, Locale locale) {
        List<LoggedEntry> entries = log.getEntries();
        return IntStream.range(0, entries.size())
                .mapToObj(position -> toResponse(
                        position, entries.get(position), library.get(entries.get(position).exercise()), log, locale))
                .toList();
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

    ClientOverviewResponse toResponse(ClientOverview overview) {
        return new ClientOverviewResponse(
                overview.getToday(),
                overview.getWeek()
                        .map(week -> new ClientOverviewResponse.OverviewWeekResponse(
                                week.number(), week.startsOn(), toResponses(week.sessions())))
                        .orElse(null),
                toResponses(overview.getRecentSessions()),
                overview.getQuizAnswers().map(this::toResponse).orElse(null));
    }

    private List<ClientOverviewResponse.ClientSessionResponse> toResponses(List<ClientSession> sessions) {
        return sessions.stream()
                .map(session -> new ClientOverviewResponse.ClientSessionResponse(
                        session.getDate(),
                        session.getDate().getDayOfWeek(),
                        session.getName(),
                        session.getState().orElse(null),
                        session.isLocked(),
                        session.getSetsLogged(),
                        session.getSetsPlanned(),
                        session.getAverageRpe().orElse(null),
                        session.getSessionLogId().map(SessionLogId::value).orElse(null)))
                .toList();
    }

    // likes in their declared order, as the client feature answers them
    private ClientOverviewResponse.OverviewQuizAnswersResponse toResponse(QuizSubmission answers) {
        return new ClientOverviewResponse.OverviewQuizAnswersResponse(
                answers.getGoal(),
                answers.getExperience(),
                answers.getLevel(),
                answers.getAge(),
                answers.getHeightCm(),
                answers.getWeightKg(),
                answers.getLikes().stream().sorted().toList(),
                answers.getDaysPerWeek(),
                answers.getNotes().orElse(null),
                answers.getSubmittedAt());
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
