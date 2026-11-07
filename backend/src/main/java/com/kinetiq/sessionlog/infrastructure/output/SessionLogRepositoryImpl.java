package com.kinetiq.sessionlog.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLog;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import com.kinetiq.sessionlog.domain.output.SessionLogRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class SessionLogRepositoryImpl implements SessionLogRepository {

    private final JpaSessionLogRepository jpa;
    private final SessionLogMapper mapper;

    @Override
    public Optional<SessionLog> findByClientAndDate(AccountId client, LocalDate date) {
        return jpa.findByClientIdAndLogDate(client.value(), date).map(mapper::toDomain);
    }

    @Override
    public Optional<SessionLog> findByIdAndClient(SessionLogId id, AccountId client) {
        return jpa.findByIdAndClientId(id.value(), client.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<SessionLogId> sessionOfSetLog(SetLogId id) {
        return jpa.findSessionIdOfSetLog(id.value()).map(SessionLogId::new);
    }

    @Override
    public SessionLog save(SessionLog log) {
        // a new session is flushed alone first: Hibernate writes its entries after the set logs it is given with
        SessionLogEntity entity = jpa.findById(log.getId().value()).orElseGet(() -> jpa.saveAndFlush(insert(log)));
        syncSetLogs(log, entity);
        return mapper.toDomain(jpa.saveAndFlush(entity));
    }

    // entries never change after the start, so they are copied once
    private SessionLogEntity insert(SessionLog log) {
        SessionLogEntity entity = new SessionLogEntity();
        entity.setId(log.getId().value());
        entity.setClientId(log.getClient().value());
        entity.setLogDate(log.getDate());
        entity.setSessionName(log.getName());
        entity.setStartedAt(log.getStartedAt());
        entity.setFinishedAt(log.getFinishedAt().orElse(null));
        log.getEntries().forEach(entry -> entity.getEntries().add(mapper.toEmbeddable(entry)));
        return entity;
    }

    // a set log is added or has its values replaced; its place and its targets copy stay
    private void syncSetLogs(SessionLog log, SessionLogEntity entity) {
        Map<UUID, SetLogEntity> stored =
                entity.getSetLogs().stream().collect(Collectors.toMap(SetLogEntity::getId, Function.identity()));
        for (SetLog setLog : log.getSetLogs()) {
            SetLogEntity existing = stored.get(setLog.id().value());
            if (existing == null) {
                entity.getSetLogs().add(mapper.toEntity(setLog, entity));
            } else {
                mapper.copyValues(setLog, existing);
            }
        }
    }
}
