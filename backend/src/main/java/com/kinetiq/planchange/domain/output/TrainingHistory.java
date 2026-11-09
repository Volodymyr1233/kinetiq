package com.kinetiq.planchange.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.ScheduledClient;
import com.kinetiq.planchange.domain.model.WeekTraining;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Read access to the plans and logs of clients, and the record of the weeks already checked. */
public interface TrainingHistory {

    List<ScheduledClient> clientsWithPublishedPlan();

    /** Empty when the client has no published plan. */
    Optional<ScheduledClient> scheduledClient(AccountId client);

    /** The client's own {@code count} consecutive weeks from {@code firstMonday}, oldest first. */
    List<WeekTraining> weeks(AccountId client, LocalDate firstMonday, int count);

    /** Records the week as checked; {@code false} when it already was. */
    boolean claimWeek(AccountId client, LocalDate week, Instant now);
}
