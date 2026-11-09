package com.kinetiq.planchange.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.input.DetectPlanChangesUseCase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanChangeSweepTest {

    private static final AccountId FIRST = AccountId.random();
    private static final AccountId BROKEN = AccountId.random();
    private static final AccountId LAST = AccountId.random();

    private final List<AccountId> checked = new ArrayList<>();

    private final DetectPlanChangesUseCase detection = new DetectPlanChangesUseCase() {
        @Override
        public List<AccountId> due() {
            return List.of(FIRST, BROKEN, LAST);
        }

        @Override
        public void check(AccountId client) {
            if (client.equals(BROKEN)) {
                throw new IllegalStateException("broken client");
            }
            checked.add(client);
        }
    };

    @Test
    void shouldCheckTheOtherClients_whenOneFails() {
        new PlanChangeSweep(detection).checkDue();

        assertThat(checked).containsExactly(FIRST, LAST);
    }
}
