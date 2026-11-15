package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.planchange.domain.model.PlanChangeState;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaPlanChangeRepository extends JpaRepository<PlanChangeEntity, UUID> {

    @Query("""
            select count(c) > 0 from PlanChangeEntity c where c.clientId = :clientId
            and (c.state in :open or (c.state = com.kinetiq.planchange.domain.model.PlanChangeState.APPLIED
                                      and c.tookEffectAt is null))
            """)
    boolean existsUnsettled(UUID clientId, Collection<PlanChangeState> open);

    // the effective week has begun when it is not after the date it now is for the client, who lives in their own zone
    @Query(
            nativeQuery = true,
            value = """
                    select c.id from plan_changes c join accounts a on a.id = c.client_id
                    where (c.state in ('PENDING', 'ACCEPTED_BY_ONE') or (c.state = 'APPLIED' and c.took_effect_at is null))
                      and c.effective_week <= (cast(:now as timestamptz) at time zone a.time_zone)::date
                    order by c.effective_week, c.id
                    """)
    List<UUID> findDueIds(Instant now);

    Optional<PlanChangeEntity> findByClientIdAndStateIn(UUID clientId, Collection<PlanChangeState> states);

    @Query("select c from PlanChangeEntity c where c.clientId = :clientId and c.state in :states order by coalesce(c.endedAt, c.createdAt) desc")
    List<PlanChangeEntity> findLatestIn(UUID clientId, Collection<PlanChangeState> states, Pageable page);

    @Query("select max(change.endedAt) from PlanChangeEntity change where change.clientId = :clientId")
    Optional<Instant> lastEndedAt(UUID clientId);
}
