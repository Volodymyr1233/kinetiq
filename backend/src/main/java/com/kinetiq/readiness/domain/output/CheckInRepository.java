package com.kinetiq.readiness.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.readiness.domain.model.CheckIn;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckInRepository {

    /** Finds the check-in the client gave for {@code date}. */
    Optional<CheckIn> findByClientAndDate(AccountId client, LocalDate date);

    /** The client's check-ins from {@code from} to {@code to}, both included, oldest first. */
    List<CheckIn> findAllByClientAndDateBetween(AccountId client, LocalDate from, LocalDate to);

    /** Inserts the check-in or replaces the stored one, and returns it as stored. */
    CheckIn save(CheckIn checkIn);
}
