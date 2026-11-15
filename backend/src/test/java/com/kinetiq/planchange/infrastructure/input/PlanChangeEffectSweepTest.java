package com.kinetiq.planchange.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.planchange.domain.input.ApplyPlanChangesUseCase;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanChangeEffectSweepTest {

    private static final PlanChangeId FIRST = PlanChangeId.random();
    private static final PlanChangeId BROKEN = PlanChangeId.random();
    private static final PlanChangeId LAST = PlanChangeId.random();

    private final List<PlanChangeId> settled = new ArrayList<>();

    private final ApplyPlanChangesUseCase settlement = new ApplyPlanChangesUseCase() {
        @Override
        public List<PlanChangeId> due() {
            return List.of(FIRST, BROKEN, LAST);
        }

        @Override
        public void settle(PlanChangeId id) {
            if (id.equals(BROKEN)) {
                throw new IllegalStateException("broken plan change");
            }
            settled.add(id);
        }
    };

    @Test
    void shouldSettleTheOtherPlanChanges_whenOneFails() {
        new PlanChangeEffectSweep(settlement).settleDue();

        assertThat(settled).containsExactly(FIRST, LAST);
    }
}
