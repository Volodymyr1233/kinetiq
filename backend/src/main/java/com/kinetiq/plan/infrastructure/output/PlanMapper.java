package com.kinetiq.plan.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanId;
import com.kinetiq.plan.domain.model.Session;
import java.util.ArrayList;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface PlanMapper {

    // written by hand because the typed ids and the nested sessions are converted in both directions
    default Plan toDomain(PlanEntity entity) {
        return new Plan(
                new PlanId(entity.getId()),
                new AccountId(entity.getClientId()),
                entity.getGoal(),
                entity.getWeeks(),
                entity.getFirstWeek(),
                entity.getSessions().stream().map(this::toDomain).toList(),
                entity.getVersion(),
                entity.getAutomaticDraft());
    }

    private Session toDomain(PlanSessionEntity entity) {
        return new Session(
                entity.getName(),
                entity.getDay(),
                entity.getEntries().stream().map(this::toDomain).toList());
    }

    private ExerciseEntry toDomain(PlanEntryEmbeddable entry) {
        return new ExerciseEntry(
                new ExerciseId(entry.getExerciseId()),
                entry.getSets(),
                entry.getAmount(),
                entry.getLoadKg(),
                entry.getRpe(),
                entry.getRestSeconds());
    }

    default PlanSessionEntity toEntity(Session session, PlanEntity plan) {
        PlanSessionEntity entity = new PlanSessionEntity();
        // the API has no session ids, because every save replaces the sessions
        entity.setId(UUID.randomUUID());
        entity.setPlan(plan);
        entity.setName(session.name());
        entity.setDay(session.day());
        entity.setEntries(new ArrayList<>(session.entries().stream().map(this::toEmbeddable).toList()));
        return entity;
    }

    private PlanEntryEmbeddable toEmbeddable(ExerciseEntry entry) {
        PlanEntryEmbeddable embeddable = new PlanEntryEmbeddable();
        embeddable.setExerciseId(entry.exercise().value());
        embeddable.setSets(entry.sets());
        embeddable.setAmount(entry.amount());
        embeddable.setLoadKg(entry.loadKg());
        embeddable.setRpe(entry.rpe());
        embeddable.setRestSeconds(entry.restSeconds());
        return embeddable;
    }
}
