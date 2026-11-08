package com.kinetiq.plan.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.exception.PlanChangedException;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.output.PlanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PlanRepositoryImpl implements PlanRepository {

    private static final String UNIQUE_CLIENT = "uq_plans_client";

    private final JpaPlanRepository jpa;
    private final PlanMapper mapper;
    private final EntityManager entityManager;

    @Override
    public Optional<Plan> findByClient(AccountId client) {
        return jpa.findByClientId(client.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Plan> findByClientForUpdate(AccountId client) {
        return jpa.findByClientIdForUpdate(client.value()).map(mapper::toDomain);
    }

    @Override
    public boolean isSeenByClient(AccountId client) {
        return jpa.existsByClientIdAndClientSeenAtIsNotNull(client.value());
    }

    @Override
    public void markSeenByClient(AccountId client, Instant at) {
        jpa.markSeen(client.value(), at);
    }

    @Override
    public Plan save(Plan plan) {
        Optional<PlanEntity> stored = jpa.findById(plan.getId().value());
        return mapper.toDomain(stored.isPresent() ? replace(stored.get(), plan) : insert(plan));
    }

    // flushed at once, so a first save that lost a race on the client's unique constraint fails here, where it can be
    // told apart from other failures
    private PlanEntity insert(Plan plan) {
        PlanEntity entity = new PlanEntity();
        entity.setId(plan.getId().value());
        entity.setClientId(plan.getClient().value());
        copyValues(plan, entity);
        addSessions(plan, entity);
        try {
            return jpa.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException violation
                    && UNIQUE_CLIENT.equals(violation.getConstraintName())) {
                throw new PlanChangedException();
            }
            throw exception;
        }
    }

    private PlanEntity replace(PlanEntity entity, Plan plan) {
        copyValues(plan, entity);
        // Hibernate inserts before it deletes, which would break the unique day of a session kept on its day
        entity.getSessions().clear();
        entityManager.flush();
        addSessions(plan, entity);
        entityManager.flush();
        // a change that touches only an entry does not make the plan dirty; the response needs the new version now,
        // which the optimistic mode would only set at commit
        entityManager.lock(entity, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        return entity;
    }

    private static void copyValues(Plan plan, PlanEntity entity) {
        entity.setGoal(plan.getGoal());
        entity.setWeeks(plan.getWeeks());
        entity.setFirstWeek(plan.getFirstWeek().orElse(null));
        entity.setPublishedOn(plan.getPublishedOn().orElse(null));
        entity.setAutomaticDraft(plan.getAutomaticDraft().orElse(null));
    }

    private void addSessions(Plan plan, PlanEntity entity) {
        plan.getSessions().forEach(session -> entity.getSessions().add(mapper.toEntity(session, entity)));
    }
}
