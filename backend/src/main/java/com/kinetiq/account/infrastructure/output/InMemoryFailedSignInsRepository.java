package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.FailedSignIns;
import com.kinetiq.account.domain.output.FailedSignInsRepository;
import com.kinetiq.common.domain.model.Email;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Keeps failed sign-ins in memory: losing them on restart is accepted (spec, Edge cases), and every attempt costs a
 * password hash, which caps how fast entries can pile up.
 */
// ponytail: counts are per instance, and parallel attempts for one email may slip a few past five because find and
// save are separate; a table with an atomic update is the upgrade for either
@Repository
class InMemoryFailedSignInsRepository implements FailedSignInsRepository {

    private static final int PRUNE_ABOVE = 10_000;

    private final Map<Email, FailedSignIns> byEmail = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    @Override
    public Optional<FailedSignIns> find(Email email) {
        return Optional.ofNullable(byEmail.get(email));
    }

    @Override
    public void save(FailedSignIns failedSignIns) {
        if (byEmail.size() > PRUNE_ABOVE) {
            byEmail.values().removeIf(entry -> entry.isExpired(clock.instant()));
        }
        byEmail.put(failedSignIns.getEmail(), failedSignIns);
    }

    @Override
    public void delete(Email email) {
        byEmail.remove(email);
    }
}
