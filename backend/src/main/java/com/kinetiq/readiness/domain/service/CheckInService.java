package com.kinetiq.readiness.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Notice;
import com.kinetiq.common.domain.model.NoticeKind;
import com.kinetiq.common.domain.output.NoticeBoard;
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.readiness.domain.exception.NoAdjustmentException;
import com.kinetiq.readiness.domain.exception.NoSessionTodayException;
import com.kinetiq.readiness.domain.exception.TrainingStartedException;
import com.kinetiq.readiness.domain.input.CheckInUseCase;
import com.kinetiq.readiness.domain.input.ChooseTargetsCommand;
import com.kinetiq.readiness.domain.input.SaveCheckInCommand;
import com.kinetiq.readiness.domain.input.TodayCheckIn;
import com.kinetiq.readiness.domain.model.Readiness;
import com.kinetiq.readiness.domain.model.Adjustment;
import com.kinetiq.readiness.domain.model.CheckIn;
import com.kinetiq.readiness.domain.model.Planned;
import com.kinetiq.readiness.domain.model.Target;
import com.kinetiq.readiness.domain.model.TargetChoice;
import com.kinetiq.readiness.domain.output.CheckInRepository;
import com.kinetiq.readiness.domain.output.TrainingProgress;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CheckInService implements CheckInUseCase {

    private final ViewOwnPlanUseCase plan;
    private final TrainingProgress training;
    private final CheckInRepository checkIns;
    private final CurrentAccountUseCase accounts;
    private final NoticeBoard notices;
    private final Clock clock;

    @Override
    public TodayCheckIn today(AccountId client) {
        TodayView today = plan.today(client);
        boolean open = today.state() == TodayView.State.SESSION && !training.hasLoggedSet(client, today.date());
        return answer(client, today, open, checkIns.findByClientAndDate(client, today.date()).orElse(null));
    }

    // ponytail: a set logged in the same instant as a save can let both through; one person, milliseconds apart, and
    // the adjustment is derived on every read, so no lock across features
    @Override
    public TodayCheckIn save(SaveCheckInCommand command) {
        AccountId client = command.client();
        TodayView today = openToday(client);
        Optional<CheckIn> previous = checkIns.findByClientAndDate(client, today.date());
        boolean wasRecoveryDay = previous.filter(CheckInService::isRecoveryDay).isPresent();
        CheckIn checkIn = previous.map(stored -> {
                    stored.change(command.answers(), clock.instant());
                    return stored;
                })
                .orElseGet(() -> CheckIn.give(client, today.date(), command.answers(), clock.instant()));
        CheckIn saved = checkIns.save(checkIn);
        // once a day: saving the same answers again, or other answers that are still a recovery day, tells nobody
        if (isRecoveryDay(saved) && !wasRecoveryDay) {
            accounts.find(client)
                    .getTrainerId()
                    .ifPresent(trainer ->
                            notices.raise(new Notice(trainer.value(), client.value(), NoticeKind.RECOVERY_DAY)));
        }
        return answer(client, today, true, saved);
    }

    // ponytail: a switch in the same instant as the first logged set can let both through, the same ceiling as save
    @Override
    public TodayCheckIn choose(ChooseTargetsCommand command) {
        AccountId client = command.client();
        TodayView today = openToday(client);
        CheckIn checkIn =
                checkIns.findByClientAndDate(client, today.date()).orElseThrow(NoAdjustmentException::new);
        if (adjustment(client, checkIn, planned(today)).isEmpty()) {
            throw new NoAdjustmentException();
        }
        checkIn.choose(command.choice());
        return answer(client, today, true, checkIns.save(checkIn));
    }

    @Override
    public Optional<Adjustment> adjustmentOn(AccountId client, LocalDate date, List<Planned> entries) {
        return checkIns.findByClientAndDate(client, date).flatMap(checkIn -> adjustment(client, checkIn, entries));
    }

    @Override
    public List<CheckIn> between(AccountId client, LocalDate from, LocalDate to) {
        return checkIns.findAllByClientAndDateBetween(client, from, to);
    }

    private static boolean isRecoveryDay(CheckIn checkIn) {
        return checkIn.readiness().band() == Readiness.Band.RECOVERY_DAY;
    }

    private TodayView openToday(AccountId client) {
        TodayView today = plan.today(client);
        if (today.state() != TodayView.State.SESSION) {
            throw new NoSessionTodayException();
        }
        if (training.hasLoggedSet(client, today.date())) {
            throw new TrainingStartedException();
        }
        return today;
    }

    private TodayCheckIn answer(AccountId client, TodayView today, boolean open, CheckIn checkIn) {
        Adjustment adjustment = checkIn == null || today.state() != TodayView.State.SESSION
                ? null
                : adjustment(client, checkIn, planned(today)).orElse(null);
        return new TodayCheckIn(today.date(), open, checkIn, adjustment);
    }

    private Optional<Adjustment> adjustment(AccountId client, CheckIn checkIn, List<Planned> entries) {
        return Adjustment.of(
                checkIn.getAnswers(),
                checkIn.getTargetChoice() == TargetChoice.LIGHTER,
                entries,
                accounts.find(client).getSettings().units());
    }

    private static List<Planned> planned(TodayView today) {
        return today.session().entries().stream()
                .map(entry -> new Planned(
                        new Target(entry.loadKg(), entry.amount(), entry.rpe()),
                        today.library().get(entry.exercise()).getMeasure()))
                .toList();
    }
}
