package com.kinetiq.sessionlog.infrastructure.output;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaSessionLogRepository extends JpaRepository<SessionLogEntity, UUID> {

    Optional<SessionLogEntity> findByClientIdAndLogDate(UUID clientId, LocalDate logDate);

    Optional<SessionLogEntity> findByIdAndClientId(UUID id, UUID clientId);

    @Query(
            """
            select new com.kinetiq.sessionlog.infrastructure.output.HeaderRow(
                log.id, log.logDate, log.startedAt, log.finishedAt)
            from SessionLogEntity log
            where log.clientId = :clientId and log.logDate between :from and :to
            """)
    List<HeaderRow> findHeaders(UUID clientId, LocalDate from, LocalDate to);

    @Query("select setLog.sessionLog.id from SetLogEntity setLog where setLog.id = :setLogId")
    Optional<UUID> findSessionIdOfSetLog(UUID setLogId);
}
