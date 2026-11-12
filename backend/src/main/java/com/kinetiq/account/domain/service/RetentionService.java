package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.input.EraseExpiredDataUseCase;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.InviteRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RetentionService implements EraseExpiredDataUseCase {

    // a link is valid 24 hours from sending, so 6 days after it expired is 7 days after it was sent
    static final Duration UNCONFIRMED_AFTER_LINK_EXPIRY = Duration.ofDays(6);
    static final Duration ENDED_INVITE = Duration.ofHours(1);

    private final AccountRepository accounts;
    private final InviteRepository invites;
    private final Clock clock;

    @Override
    public void eraseExpired() {
        Instant now = clock.instant();
        int trainers = accounts.deleteUnconfirmedTrainers(now.minus(UNCONFIRMED_AFTER_LINK_EXPIRY));
        int ended = invites.deleteEnded(now.minus(ENDED_INVITE));
        if (trainers > 0 || ended > 0) {
            log.info("Expired data erased, unconfirmedTrainers={}, invites={}", trainers, ended);
        }
    }
}
