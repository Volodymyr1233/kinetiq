package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.InviteId;
import com.kinetiq.account.domain.model.InviteStatus;
import com.kinetiq.account.domain.output.InviteRepository;
import com.kinetiq.common.domain.model.Email;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryInviteRepository implements InviteRepository {

    final Map<InviteId, Invite> byId = new LinkedHashMap<>();
    // the domain does not know when a used or revoked invite ended, so the cut-off is recorded for the test
    Instant endedBefore;

    @Override
    public void save(Invite invite) {
        byId.put(invite.getId(), invite);
    }

    @Override
    public Optional<Invite> findById(InviteId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Invite> findOpenByTrainer(AccountId trainerId, Instant now) {
        return byId.values().stream()
                .filter(invite -> invite.getTrainerId().equals(trainerId) && invite.isOpen(now))
                .sorted(Comparator.comparing(Invite::getSentAt).reversed())
                .toList();
    }

    @Override
    public Optional<Invite> findOpenByTrainerAndEmail(AccountId trainerId, Email email) {
        return byId.values().stream()
                .filter(invite -> invite.getTrainerId().equals(trainerId)
                        && invite.getEmail().equals(email)
                        && invite.getStatus() == InviteStatus.OPEN)
                .findFirst();
    }

    @Override
    public Optional<Invite> findByTokenHash(String tokenHash) {
        return byId.values().stream()
                .filter(invite -> invite.getEmailTokenHash().equals(tokenHash)
                        || invite.getLinkTokenHash().equals(tokenHash))
                .findFirst();
    }

    @Override
    public int deleteEnded(Instant before) {
        endedBefore = before;
        int count = byId.size();
        byId.values().removeIf(invite -> invite.getStatus() == InviteStatus.OPEN
                && invite.getExpiresAt().isBefore(before));
        return count - byId.size();
    }

    @Override
    public void deleteByTrainerAndEmail(AccountId trainerId, Email email) {
        byId.values().removeIf(invite -> invite.getTrainerId().equals(trainerId) && invite.getEmail().equals(email));
    }
}
