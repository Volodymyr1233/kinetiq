package com.kinetiq.overview.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.overview.domain.model.ClientTraining;
import java.time.LocalDate;
import java.util.Map;

/** Read access to the training of all of one trainer's clients, for the trainer's overview. */
public interface TrainingRecords {

    /**
     * The training of each of the trainer's clients that has any: the published plan's dates, the session logs and
     * check-ins dated {@code since} or later, and the newest logged set ever. Nobody else's clients appear.
     */
    Map<AccountId, ClientTraining> forTrainer(AccountId trainer, LocalDate since);
}
