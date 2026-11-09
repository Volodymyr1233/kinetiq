package com.kinetiq.readiness.infrastructure.input;

import com.kinetiq.readiness.domain.input.TodayCheckIn;
import com.kinetiq.readiness.domain.model.Adjustment;
import com.kinetiq.readiness.domain.model.CheckIn;
import com.kinetiq.readiness.domain.model.Readiness;
import com.kinetiq.readiness.domain.model.Target;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
class CheckInWebMapper {

    TodayCheckInResponse toResponse(TodayCheckIn today) {
        return new TodayCheckInResponse(
                today.date(),
                today.open(),
                today.checkIn() == null ? null : toResponse(today.checkIn(), today.adjustment()));
    }

    private TodayCheckInResponse.CheckInResponse toResponse(CheckIn checkIn, Adjustment adjustment) {
        Readiness readiness = checkIn.readiness();
        return new TodayCheckInResponse.CheckInResponse(
                checkIn.getAnswers().sleep(),
                checkIn.getAnswers().soreness(),
                checkIn.getAnswers().stress(),
                readiness.score(),
                readiness.band(),
                checkIn.getSavedAt(),
                adjustment == null ? null : toResponse(adjustment));
    }

    private TodayCheckInResponse.AdjustmentResponse toResponse(Adjustment adjustment) {
        List<Target> lighter = adjustment.lighter();
        return new TodayCheckInResponse.AdjustmentResponse(
                adjustment.inUse(),
                adjustment.causes(),
                IntStream.range(0, lighter.size())
                        .mapToObj(position -> new TodayCheckInResponse.AdjustedTargetResponse(
                                position,
                                lighter.get(position).loadKg(),
                                lighter.get(position).amount(),
                                lighter.get(position).rpe()))
                        .toList());
    }
}
