package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.RemovedSession;
import com.kinetiq.planchange.domain.model.SessionMove;
import com.kinetiq.planchange.domain.model.TargetChange;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface PlanChangeMapper {

    // written by hand because the typed ids and the nested content are converted in both directions
    default PlanChange toDomain(PlanChangeEntity entity) {
        Optional<RemovedSession> removed = Optional.ofNullable(entity.getRemovedDay())
                .map(day -> new RemovedSession(day, entity.getRemovedName()));
        PlanChangeContent content = new PlanChangeContent(
                entity.getTargets().stream()
                        .map(this::toDomain)
                        .sorted(Comparator.comparing((TargetChange change) -> change.entry().day())
                                .thenComparing(change -> change.entry().position())
                                .thenComparing(TargetChange::target))
                        .toList(),
                removed,
                entity.getMoves().stream()
                        .sorted(Comparator.comparing(PlanChangeMoveEmbeddable::getFromPosition))
                        .map(move -> new SessionMove(
                                new EntryRef(entity.getRemovedDay(), move.getFromPosition()),
                                new ExerciseId(move.getExerciseId()),
                                move.getToDay()))
                        .toList());
        return new PlanChange(
                new PlanChangeId(entity.getId()),
                new AccountId(entity.getClientId()),
                entity.getRule(),
                entity.getState(),
                entity.getPlanVersion(),
                entity.getWindowStart(),
                entity.getWindowEnd(),
                entity.getEffectiveWeek(),
                content,
                entity.getFigures().stream()
                        .sorted(Comparator.comparing(PlanChangeFigureEmbeddable::getKind)
                                .thenComparing(figure -> String.valueOf(figure.getExerciseId()))
                                .thenComparing(PlanChangeFigureEmbeddable::getWeekStart))
                        .map(this::toDomain)
                        .toList(),
                entity.getExercises().stream().map(ExerciseId::new).collect(Collectors.toSet()),
                entity.getCreatedAt(),
                entity.getEndedAt());
    }

    private TargetChange toDomain(PlanChangeTargetEmbeddable target) {
        return new TargetChange(
                new EntryRef(target.getSessionDay(), target.getPosition()),
                new ExerciseId(target.getExerciseId()),
                target.getSessionName(),
                target.getTarget(),
                target.getOldValue(),
                target.getNewValue());
    }

    private Figure toDomain(PlanChangeFigureEmbeddable figure) {
        return new Figure(
                figure.getKind(),
                Optional.ofNullable(figure.getExerciseId()).map(ExerciseId::new),
                figure.getWeekStart(),
                figure.getValue());
    }

    default PlanChangeEntity toEntity(PlanChange planChange) {
        PlanChangeEntity entity = new PlanChangeEntity();
        entity.setId(planChange.getId().value());
        entity.setClientId(planChange.getClient().value());
        entity.setRule(planChange.getRule());
        entity.setState(planChange.getState());
        entity.setPlanVersion(planChange.getPlanVersion());
        entity.setWindowStart(planChange.getWindowStart());
        entity.setWindowEnd(planChange.getWindowEnd());
        entity.setEffectiveWeek(planChange.getEffectiveWeek());
        PlanChangeContent content = planChange.getContent();
        content.removed().ifPresent(removed -> {
            entity.setRemovedDay(removed.day());
            entity.setRemovedName(removed.name());
        });
        entity.setCreatedAt(planChange.getCreatedAt());
        entity.setEndedAt(planChange.getEndedAt().orElse(null));
        entity.setTargets(new ArrayList<>(content.changes().stream().map(this::toEmbeddable).toList()));
        entity.setMoves(new ArrayList<>(content.moves().stream().map(this::toEmbeddable).toList()));
        entity.setFigures(new ArrayList<>(planChange.getFigures().stream().map(this::toEmbeddable).toList()));
        entity.setExercises(new HashSet<>(planChange.getConcernedExercises().stream()
                .map(ExerciseId::value)
                .toList()));
        return entity;
    }

    private PlanChangeTargetEmbeddable toEmbeddable(TargetChange change) {
        PlanChangeTargetEmbeddable embeddable = new PlanChangeTargetEmbeddable();
        embeddable.setSessionDay(change.entry().day());
        embeddable.setPosition(change.entry().position());
        embeddable.setTarget(change.target());
        embeddable.setExerciseId(change.exercise().value());
        embeddable.setSessionName(change.sessionName());
        embeddable.setOldValue(change.oldValue());
        embeddable.setNewValue(change.newValue());
        return embeddable;
    }

    private PlanChangeMoveEmbeddable toEmbeddable(SessionMove move) {
        PlanChangeMoveEmbeddable embeddable = new PlanChangeMoveEmbeddable();
        embeddable.setFromPosition(move.from().position());
        embeddable.setExerciseId(move.exercise().value());
        embeddable.setToDay(move.toDay());
        return embeddable;
    }

    private PlanChangeFigureEmbeddable toEmbeddable(Figure figure) {
        PlanChangeFigureEmbeddable embeddable = new PlanChangeFigureEmbeddable();
        embeddable.setId(UUID.randomUUID());
        embeddable.setKind(figure.kind());
        embeddable.setExerciseId(figure.exercise().map(ExerciseId::value).orElse(null));
        embeddable.setWeekStart(figure.weekStart());
        embeddable.setValue(figure.value());
        return embeddable;
    }
}
