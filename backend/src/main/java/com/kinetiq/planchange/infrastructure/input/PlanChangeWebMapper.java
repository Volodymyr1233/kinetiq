package com.kinetiq.planchange.infrastructure.input;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.planchange.domain.input.ClientPlanChangeView;
import com.kinetiq.planchange.domain.input.PlanChangeSummary;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.model.Decision;
import com.kinetiq.planchange.domain.model.ExerciseLabel;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.Side;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface PlanChangeWebMapper {

    // written by hand because the nested content takes its names from the view
    default PlanChangeResponse toResponse(PlanChangeView view) {
        PlanChange planChange = view.planChange();
        PlanChangeContent content = planChange.getContent();
        Map<ExerciseId, ExerciseLabel> exercises = view.exercises();
        Decision decision = planChange.getDecision().orElse(null);
        return new PlanChangeResponse(
                planChange.getId().value(),
                planChange.getClient().value(),
                view.clientName(),
                planChange.getRule(),
                planChange.getState(),
                view.text().title(),
                view.text().reason(),
                planChange.getEffectiveWeek(),
                planChange.getWindowStart(),
                planChange.getWindowEnd(),
                targets(content, exercises),
                removedSession(content),
                moves(content, exercises),
                concernedExercises(planChange, exercises),
                figures(planChange),
                planChange.acceptedBy(Side.TRAINER),
                planChange.acceptedBy(Side.CLIENT),
                decision == null ? null : decision.by(),
                decision == null ? null : decision.reason(),
                decision == null ? null : decision.note(),
                planChange.getEndedAt().orElse(null));
    }

    default ClientPlanChangeDetailResponse toResponse(ClientPlanChangeView view) {
        PlanChange planChange = view.planChange();
        PlanChangeContent content = planChange.getContent();
        Map<ExerciseId, ExerciseLabel> exercises = view.exercises();
        Decision decision = planChange.getDecision().orElse(null);
        return new ClientPlanChangeDetailResponse(
                planChange.getId().value(),
                view.counterpartName(),
                planChange.getRule(),
                planChange.getState(),
                view.text().title(),
                view.text().reason(),
                planChange.getEffectiveWeek(),
                planChange.getWindowStart(),
                planChange.getWindowEnd(),
                targets(content, exercises),
                removedSession(content),
                moves(content, exercises),
                concernedExercises(planChange, exercises),
                figures(planChange),
                planChange.acceptedBy(Side.TRAINER),
                planChange.acceptedBy(Side.CLIENT),
                decision == null ? null : decision.by(),
                decision == null ? null : decision.reason(),
                decision == null ? null : decision.note(),
                planChange.getEndedAt().orElse(null));
    }

    default ClientPlanChangeResponse toResponse(PlanChangeSummary summary) {
        PlanChange planChange = summary.planChange();
        return new ClientPlanChangeResponse(
                planChange.getId().value(),
                planChange.getRule(),
                planChange.getState(),
                summary.text().title(),
                summary.text().reason(),
                planChange.getEffectiveWeek(),
                planChange.acceptedBy(Side.TRAINER),
                planChange.acceptedBy(Side.CLIENT));
    }

    private static List<PlanChangeResponse.PlanChangeTargetResponse> targets(
            PlanChangeContent content, Map<ExerciseId, ExerciseLabel> exercises) {
        return content.changes().stream()
                .map(change -> new PlanChangeResponse.PlanChangeTargetResponse(
                        change.entry().day(),
                        change.sessionName(),
                        change.entry().position(),
                        change.exercise().value(),
                        name(exercises, change.exercise()),
                        measure(exercises, change.exercise()),
                        change.target(),
                        change.oldValue(),
                        change.newValue()))
                .sorted(Comparator.comparing(PlanChangeResponse.PlanChangeTargetResponse::sessionDay)
                        .thenComparing(PlanChangeResponse.PlanChangeTargetResponse::position)
                        .thenComparing(PlanChangeResponse.PlanChangeTargetResponse::target))
                .toList();
    }

    private static PlanChangeResponse.PlanChangeRemovedSessionResponse removedSession(PlanChangeContent content) {
        return content.removed()
                .map(removed -> new PlanChangeResponse.PlanChangeRemovedSessionResponse(removed.day(), removed.name()))
                .orElse(null);
    }

    private static List<PlanChangeResponse.PlanChangeMoveResponse> moves(
            PlanChangeContent content, Map<ExerciseId, ExerciseLabel> exercises) {
        return content.moves().stream()
                .map(move -> new PlanChangeResponse.PlanChangeMoveResponse(
                        move.exercise().value(), name(exercises, move.exercise()), move.from().day(), move.toDay()))
                .toList();
    }

    private static List<PlanChangeResponse.PlanChangeExerciseResponse> concernedExercises(
            PlanChange planChange, Map<ExerciseId, ExerciseLabel> exercises) {
        return planChange.getConcernedExercises().stream()
                .map(exercise ->
                        new PlanChangeResponse.PlanChangeExerciseResponse(exercise.value(), name(exercises, exercise)))
                .sorted(Comparator.comparing(PlanChangeResponse.PlanChangeExerciseResponse::name))
                .toList();
    }

    private static List<PlanChangeResponse.PlanChangeFigureResponse> figures(PlanChange planChange) {
        return planChange.getFigures().stream()
                .map(figure -> new PlanChangeResponse.PlanChangeFigureResponse(
                        figure.kind(),
                        figure.exercise().map(ExerciseId::value).orElse(null),
                        figure.weekStart(),
                        figure.value()))
                .toList();
    }

    // an exercise deleted from the library since keeps a place in the table
    private static String name(Map<ExerciseId, ExerciseLabel> exercises, ExerciseId exercise) {
        ExerciseLabel label = exercises.get(exercise);
        return label == null ? "" : label.name();
    }

    private static Measure measure(Map<ExerciseId, ExerciseLabel> exercises, ExerciseId exercise) {
        ExerciseLabel label = exercises.get(exercise);
        return label == null ? Measure.REPS : label.measure();
    }
}
