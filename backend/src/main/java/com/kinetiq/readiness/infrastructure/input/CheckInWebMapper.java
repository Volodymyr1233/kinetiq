package com.kinetiq.readiness.infrastructure.input;

import com.kinetiq.readiness.domain.input.TodayCheckIn;
import com.kinetiq.readiness.domain.model.CheckIn;
import com.kinetiq.readiness.domain.model.Readiness;
import org.springframework.stereotype.Component;

@Component
class CheckInWebMapper {

    TodayCheckInResponse toResponse(TodayCheckIn today) {
        return new TodayCheckInResponse(
                today.date(), today.open(), today.checkIn() == null ? null : toResponse(today.checkIn()));
    }

    private TodayCheckInResponse.CheckInResponse toResponse(CheckIn checkIn) {
        Readiness readiness = checkIn.readiness();
        return new TodayCheckInResponse.CheckInResponse(
                checkIn.getAnswers().sleep(),
                checkIn.getAnswers().soreness(),
                checkIn.getAnswers().stress(),
                readiness.score(),
                readiness.band(),
                checkIn.getSavedAt());
    }
}
