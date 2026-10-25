package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.InviteId;
import com.kinetiq.account.domain.model.InviteStatus;
import com.kinetiq.account.domain.output.InviteRepository;
import com.kinetiq.common.domain.model.Email;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class InviteRepositoryImpl implements InviteRepository {

    private final JpaInviteRepository jpa;
    private final InviteMapper mapper;

    // updates the managed entity, so its version and timestamps survive and optimistic locking applies;
    // flushed at once, because Hibernate inserts before it updates and the one-open-invite index would refuse
    // a replacement inserted before the invite it supersedes is revoked
    @Override
    public void save(Invite invite) {
        InviteEntity entity = jpa.findById(invite.getId().value()).orElseGet(InviteEntity::new);
        mapper.update(invite, entity);
        jpa.saveAndFlush(entity);
    }

    @Override
    public Optional<Invite> findById(InviteId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Invite> findOpenByTrainer(AccountId trainerId, Instant now) {
        return jpa.findByTrainerIdAndStatusAndExpiresAtAfterOrderBySentAtDesc(trainerId.value(), InviteStatus.OPEN, now)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Invite> findOpenByTrainerAndEmail(AccountId trainerId, Email email) {
        return jpa.findByTrainerIdAndEmailAndStatus(trainerId.value(), email.value(), InviteStatus.OPEN)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Invite> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(mapper::toDomain);
    }
}
