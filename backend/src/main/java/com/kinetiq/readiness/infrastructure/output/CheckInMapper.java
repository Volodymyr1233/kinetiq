package com.kinetiq.readiness.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.readiness.domain.model.Answers;
import com.kinetiq.readiness.domain.model.CheckIn;
import com.kinetiq.readiness.domain.model.CheckInId;
import org.springframework.stereotype.Component;

// written by hand, like the session log's, because the answers are one value object over three columns
@Component
class CheckInMapper {

    CheckIn toDomain(CheckInEntity entity) {
        return new CheckIn(
                new CheckInId(entity.getId()),
                new AccountId(entity.getClientId()),
                entity.getCheckInDate(),
                new Answers(entity.getSleep(), entity.getSoreness(), entity.getStress()),
                entity.getSavedAt());
    }

    void copy(CheckIn checkIn, CheckInEntity entity) {
        entity.setId(checkIn.getId().value());
        entity.setClientId(checkIn.getClient().value());
        entity.setCheckInDate(checkIn.getDate());
        entity.setSleep((short) checkIn.getAnswers().sleep());
        entity.setSoreness((short) checkIn.getAnswers().soreness());
        entity.setStress((short) checkIn.getAnswers().stress());
        entity.setSavedAt(checkIn.getSavedAt());
    }
}
