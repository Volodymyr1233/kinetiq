package com.kinetiq.readiness.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.readiness.domain.model.CheckIn;
import com.kinetiq.readiness.domain.output.CheckInRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class CheckInRepositoryImpl implements CheckInRepository {

    private final JpaCheckInRepository jpa;
    private final CheckInMapper mapper;

    @Override
    public Optional<CheckIn> findByClientAndDate(AccountId client, LocalDate date) {
        return jpa.findByClientIdAndCheckInDate(client.value(), date).map(mapper::toDomain);
    }

    @Override
    public List<CheckIn> findAllByClientAndDateBetween(AccountId client, LocalDate from, LocalDate to) {
        return jpa.findAllByClientIdAndCheckInDateBetweenOrderByCheckInDate(client.value(), from, to).stream()
                .map(mapper::toDomain)
                .toList();
    }

    // ponytail: two first saves of one day at the same moment both insert and the second is refused by the unique
    // constraint as a 500; retry on that constraint if it ever shows
    @Override
    public CheckIn save(CheckIn checkIn) {
        CheckInEntity entity = jpa.findById(checkIn.getId().value()).orElseGet(CheckInEntity::new);
        mapper.copy(checkIn, entity);
        return mapper.toDomain(jpa.saveAndFlush(entity));
    }
}
