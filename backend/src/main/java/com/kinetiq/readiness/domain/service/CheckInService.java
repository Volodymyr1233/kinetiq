package com.kinetiq.readiness.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.readiness.domain.exception.NoSessionTodayException;
import com.kinetiq.readiness.domain.exception.TrainingStartedException;
import com.kinetiq.readiness.domain.input.CheckInUseCase;
import com.kinetiq.readiness.domain.input.SaveCheckInCommand;
import com.kinetiq.readiness.domain.input.TodayCheckIn;
import com.kinetiq.readiness.domain.model.CheckIn;
import com.kinetiq.readiness.domain.output.CheckInRepository;
import com.kinetiq.readiness.domain.output.TrainingProgress;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CheckInService implements CheckInUseCase {

    private final ViewOwnPlanUseCase plan;
    private final TrainingProgress training;
    private final CheckInRepository checkIns;
    private final Clock clock;

    @Override
    public TodayCheckIn today(AccountId client) {
        TodayView today = plan.today(client);
        boolean open = today.state() == TodayView.State.SESSION && !training.hasLoggedSet(client, today.date());
        return new TodayCheckIn(
                today.date(), open, checkIns.findByClientAndDate(client, today.date()).orElse(null));
    }

    // ponytail: a set logged in the same instant as a save can let both through; one person, milliseconds apart, and
    // #117 recomputes on change, so no lock across features
    @Override
    public TodayCheckIn save(SaveCheckInCommand command) {
        AccountId client = command.client();
        TodayView today = plan.today(client);
        if (today.state() != TodayView.State.SESSION) {
            throw new NoSessionTodayException();
        }
        if (training.hasLoggedSet(client, today.date())) {
            throw new TrainingStartedException();
        }
        CheckIn checkIn = checkIns.findByClientAndDate(client, today.date())
                .map(stored -> {
                    stored.change(command.answers(), clock.instant());
                    return stored;
                })
                .orElseGet(() -> CheckIn.give(client, today.date(), command.answers(), clock.instant()));
        return new TodayCheckIn(today.date(), true, checkIns.save(checkIn));
    }
}
