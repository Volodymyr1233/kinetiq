package com.kinetiq.readiness.infrastructure.output;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCheckInRepository extends JpaRepository<CheckInEntity, UUID> {

    Optional<CheckInEntity> findByClientIdAndCheckInDate(UUID clientId, LocalDate checkInDate);
}
