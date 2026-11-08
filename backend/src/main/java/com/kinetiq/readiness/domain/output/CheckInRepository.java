package com.kinetiq.readiness.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.readiness.domain.model.CheckIn;
import java.time.LocalDate;
import java.util.Optional;

public interface CheckInRepository {

    /** Finds the check-in the client gave for {@code date}. */
    Optional<CheckIn> findByClientAndDate(AccountId client, LocalDate date);

    /** Inserts the check-in or replaces the stored one, and returns it as stored. */
    CheckIn save(CheckIn checkIn);
}
