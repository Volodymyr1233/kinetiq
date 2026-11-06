package com.kinetiq.plan.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.PlanDetails;
import com.kinetiq.plan.domain.input.SavePlanCommand;
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.Session;
import java.util.Locale;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface PlanWebMapper {

    default PlanResponse toResponse(PlanDetails details, Locale locale) {
        Plan plan = details.plan();
        return new PlanResponse(
                plan.isPublished() ? PlanResponse.Status.PUBLISHED : PlanResponse.Status.DRAFT,
                plan.getGoal(),
                plan.getWeeks(),
                plan.getFirstWeek().orElse(null),
                details.currentWeek(),
                plan.getVersion(),
                plan.getAutomaticDraft().orElse(null),
                plan.getSessions().stream()
                        .map(session -> toResponse(session, details.library(), locale))
                        .toList());
    }

    default ClientPlanResponse toResponse(OwnPlanView view, Locale locale) {
        Plan plan = view.plan();
        return new ClientPlanResponse(
                plan.getGoal(),
                plan.getWeeks(),
                view.currentWeek(),
                view.finished(),
                view.seen(),
                view.week() == null
                        ? null
                        : new ClientPlanResponse.WeekResponse(
                                view.week().number(), view.week().startsOn()),
                plan.getSessions().stream()
                        .map(session -> toResponse(session, view.library(), locale))
                        .toList());
    }

    default TodayResponse toResponse(TodayView view, Locale locale) {
        return new TodayResponse(
                view.date(),
                view.state(),
                view.session() == null ? null : toResponse(view.session(), view.library(), locale));
    }

    private PlanResponse.SessionResponse toResponse(
            Session session, Map<ExerciseId, Exercise> library, Locale locale) {
        return new PlanResponse.SessionResponse(
                session.name(),
                session.day(),
                session.entries().stream()
                        .map(entry -> toResponse(entry, library.get(entry.exercise()), locale))
                        .toList());
    }

    private PlanResponse.EntryResponse toResponse(ExerciseEntry entry, Exercise exercise, Locale locale) {
        return new PlanResponse.EntryResponse(
                entry.exercise().value(),
                exercise.getName().in(locale),
                exercise.getMeasure(),
                entry.sets(),
                entry.amount(),
                entry.loadKg(),
                entry.rpe(),
                entry.restSeconds());
    }

    default SavePlanCommand toCommand(AccountId trainer, AccountId client, SavePlanRequest request) {
        return new SavePlanCommand(
                trainer,
                client,
                request.version(),
                request.goal(),
                request.weeks(),
                request.sessions().stream().map(PlanWebMapper::toSession).toList());
    }

    private static Session toSession(SavePlanRequest.SessionRequest session) {
        return new Session(
                session.name(),
                session.day(),
                session.entries().stream().map(PlanWebMapper::toEntry).toList());
    }

    private static ExerciseEntry toEntry(SavePlanRequest.EntryRequest entry) {
        return new ExerciseEntry(
                new ExerciseId(entry.exerciseId()),
                entry.sets(),
                entry.amount(),
                entry.loadKg(),
                entry.rpe(),
                entry.restSeconds());
    }
}
