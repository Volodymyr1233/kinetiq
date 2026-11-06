package com.kinetiq.plan.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.input.DraftInput;
import com.kinetiq.plan.domain.input.DraftPlanUseCase;
import com.kinetiq.plan.domain.model.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlanDraftingSweepTest {

    private static final AccountId FIRST = AccountId.random();
    private static final AccountId BROKEN = AccountId.random();
    private static final AccountId LAST = AccountId.random();

    private final List<AccountId> ended = new ArrayList<>();

    private final DraftPlanUseCase drafting = new DraftPlanUseCase() {
        @Override
        public List<AccountId> overdue() {
            return List.of(FIRST, BROKEN, LAST);
        }

        @Override
        public void storeFailed(AccountId client) {
            if (client.equals(BROKEN)) {
                throw new IllegalStateException("broken row");
            }
            ended.add(client);
        }

        @Override
        public void request(AccountId client) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<DraftInput> input(AccountId client) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void storeBuilt(AccountId client, AccountId trainer, List<Session> sessions) {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    void shouldEndTheOtherRows_whenOneFails() {
        new PlanDraftingSweep(drafting).endOverdue();

        assertThat(ended).containsExactly(FIRST, LAST);
    }
}
