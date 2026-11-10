package com.kinetiq.progress.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.progress.domain.model.LoggedExercise;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Read access to one client's session logs, for their progress. */
public interface TrainingLog {

    /** One per exercise in each session log of the client that has a logged set of it, oldest date first. */
    List<LoggedExercise> loggedExercises(AccountId client);

    /** The dates of the client's finished session logs. */
    Set<LocalDate> finishedDays(AccountId client);
}
