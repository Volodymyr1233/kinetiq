package com.kinetiq.sessionlog.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogHeader;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import com.kinetiq.sessionlog.domain.output.SessionLogRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** A repository that only knows headers, for tests of the states. */
class SessionLogRepositoryStub implements SessionLogRepository {

    private final List<SessionLogHeader> headers;

    SessionLogRepositoryStub(List<SessionLogHeader> headers) {
        this.headers = headers;
    }

    @Override
    public List<SessionLogHeader> headersBetween(AccountId client, LocalDate from, LocalDate to) {
        return headers.stream()
                .filter(header -> !header.date().isBefore(from) && !header.date().isAfter(to))
                .toList();
    }

    @Override
    public Optional<SessionLog> findByClientAndDate(AccountId client, LocalDate date) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<SessionLog> findByIdAndClient(SessionLogId id, AccountId client) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<SessionLogId> sessionOfSetLog(SetLogId id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionLog save(SessionLog log) {
        throw new UnsupportedOperationException();
    }
}
