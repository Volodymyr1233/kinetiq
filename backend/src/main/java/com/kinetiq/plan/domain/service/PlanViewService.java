package com.kinetiq.plan.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.exception.PlanWeekOutOfRangeException;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.output.PlanRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlanViewService implements ViewOwnPlanUseCase {

    private final CurrentAccountUseCase accounts;
    private final ListExercisesUseCase exercises;
    private final PlanRepository plans;
    private final Clock clock;

    @Override
    public OwnPlanView view(AccountId client, Integer week) {
        Plan plan = publishedPlan(client);
        Account account = accounts.find(client);
        int currentWeek = plan.currentWeek(localDate(account)).orElseThrow();
        boolean finished = currentWeek > plan.getWeeks();
        return new OwnPlanView(
                plan,
                finished ? finishedWeek(week) : shownWeek(plan, week == null ? currentWeek : week),
                currentWeek,
                finished,
                plans.isSeenByClient(client),
                library(account));
    }

    @Override
    public TodayView today(AccountId client) {
        Plan plan = publishedPlan(client);
        Account account = accounts.find(client);
        LocalDate today = localDate(account);
        if (plan.currentWeek(today).orElseThrow() > plan.getWeeks()) {
            return new TodayView(today, TodayView.State.FINISHED, null, Map.of());
        }
        // a plan has at most one session a day
        return plan.getSessions().stream()
                .filter(session -> session.day() == today.getDayOfWeek())
                .findFirst()
                .map(session -> new TodayView(today, TodayView.State.SESSION, session, library(account)))
                .orElseGet(() -> new TodayView(today, TodayView.State.REST, null, Map.of()));
    }

    @Override
    public void acknowledge(AccountId client) {
        publishedPlan(client);
        plans.markSeenByClient(client, clock.instant());
    }

    // weeks are Monday to Sunday in the client's time zone
    private LocalDate localDate(Account account) {
        return clock.instant().atZone(account.getTimeZone()).toLocalDate();
    }

    // a draft answers like no plan, before anything of it is read
    private Plan publishedPlan(AccountId client) {
        return plans.findByClient(client).filter(Plan::isPublished).orElseThrow(PlanNotFoundException::new);
    }

    // a finished plan has no week to show, and asking for one is a stale page, not a week to clamp to
    private static OwnPlanView.Week finishedWeek(Integer asked) {
        if (asked != null) {
            throw new PlanWeekOutOfRangeException();
        }
        return null;
    }

    private static OwnPlanView.Week shownWeek(Plan plan, int number) {
        if (number < 1 || number > plan.getWeeks()) {
            throw new PlanWeekOutOfRangeException();
        }
        return new OwnPlanView.Week(
                number, plan.getFirstWeek().orElseThrow().plusWeeks(number - 1L));
    }

    private Map<ExerciseId, Exercise> library(Account account) {
        return exercises.library(account.getTrainerId().orElseThrow(PlanNotFoundException::new)).stream()
                .collect(Collectors.toMap(Exercise::getId, Function.identity()));
    }
}
