package com.kinetiq.sessionlog.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.sessionlog.domain.model.LoggedEntry;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLog;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import com.kinetiq.sessionlog.domain.model.SetValues;
import org.springframework.stereotype.Component;

// written by hand because the typed ids and the nested collections are converted in both directions
@Component
class SessionLogMapper {

    SessionLog toDomain(SessionLogEntity entity) {
        return new SessionLog(
                new SessionLogId(entity.getId()),
                new AccountId(entity.getClientId()),
                entity.getLogDate(),
                entity.getSessionName(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getNote(),
                entity.getEntries().stream().map(this::toDomain).toList(),
                entity.getSetLogs().stream().map(this::toDomain).toList(),
                entity.getVersion());
    }

    private LoggedEntry toDomain(LoggedEntryEmbeddable entry) {
        return new LoggedEntry(
                new ExerciseId(entry.getExerciseId()),
                entry.getSets(),
                entry.getAmount(),
                entry.getLoadKg(),
                entry.getRpe(),
                entry.getRestSeconds());
    }

    private SetLog toDomain(SetLogEntity entity) {
        return new SetLog(
                new SetLogId(entity.getId()),
                entity.getEntryPosition(),
                entity.getSetNumber(),
                new SetValues(entity.getLoadKg(), entity.getAmount(), entity.getRpe()),
                entity.getTargetLoadKg(),
                entity.getTargetAmount(),
                entity.getTargetRpe(),
                entity.getLoggedAt());
    }

    LoggedEntryEmbeddable toEmbeddable(LoggedEntry entry) {
        LoggedEntryEmbeddable embeddable = new LoggedEntryEmbeddable();
        embeddable.setExerciseId(entry.exercise().value());
        embeddable.setSets(entry.sets());
        embeddable.setAmount(entry.amount());
        embeddable.setLoadKg(entry.loadKg());
        embeddable.setRpe(entry.rpe());
        embeddable.setRestSeconds(entry.restSeconds());
        return embeddable;
    }

    SetLogEntity toEntity(SetLog setLog, SessionLogEntity sessionLog) {
        SetLogEntity entity = new SetLogEntity();
        entity.setId(setLog.id().value());
        entity.setSessionLog(sessionLog);
        entity.setEntryPosition(setLog.entryPosition());
        entity.setSetNumber(setLog.setNumber());
        entity.setTargetLoadKg(setLog.targetLoadKg());
        entity.setTargetAmount(setLog.targetAmount());
        entity.setTargetRpe(setLog.targetRpe());
        copyValues(setLog, entity);
        return entity;
    }

    void copyValues(SetLog setLog, SetLogEntity entity) {
        entity.setLoadKg(setLog.values().loadKg());
        entity.setAmount(setLog.values().amount());
        entity.setRpe(setLog.values().rpe());
        entity.setLoggedAt(setLog.loggedAt());
    }
}
