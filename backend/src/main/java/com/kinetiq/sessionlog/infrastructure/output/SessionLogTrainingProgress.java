package com.kinetiq.sessionlog.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.readiness.domain.output.TrainingProgress;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// readiness owns the port, so sessionlog depends on readiness and never the other way round
@Component
@RequiredArgsConstructor
class SessionLogTrainingProgress implements TrainingProgress {

    private final JpaSessionLogRepository jpa;

    @Override
    public boolean hasLoggedSet(AccountId client, LocalDate date) {
        return jpa.hasSetLogOn(client.value(), date);
    }
}
