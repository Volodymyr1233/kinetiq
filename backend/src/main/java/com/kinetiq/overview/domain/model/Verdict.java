package com.kinetiq.overview.domain.model;

import com.kinetiq.client.domain.model.PlanState;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** A client's status and, when At risk or Watch, every reason that holds. */
public record Verdict(ClientStatus status, Set<StatusReason> reasons) {

    private static final int AT_RISK_BELOW = 60;
    private static final int WATCH_BELOW = 70;
    private static final BigDecimal HIGH_RPE_ABOVE = new BigDecimal("8.0");
    private static final Set<PlanState> WAITING_FOR_TRAINER =
            EnumSet.of(PlanState.PLAN_TO_REVIEW, PlanState.DRAFT, PlanState.DRAFT_FAILED);

    public Verdict {
        Objects.requireNonNull(status);
        reasons = Set.copyOf(reasons);
    }

    /**
     * The status of a client. With a running plan: At risk below 60% of due sessions done, otherwise Watch below 70% or
     * with an average RPE above 8.0, otherwise On track; without a completion or an RPE that part decides nothing.
     * Without a running plan: Plan to review while a draft or quiz answers wait for the trainer, otherwise No plan.
     * {@code draftWaiting} tells a stored draft from a first plan being built, which share the state of a rebuild.
     * {@code completion} and {@code averageRpe} may be {@code null}.
     */
    public static Verdict of(
            PlanState planState, boolean draftWaiting, boolean running, Completion completion, BigDecimal averageRpe) {
        if (!running) {
            return new Verdict(
                    draftWaiting || WAITING_FOR_TRAINER.contains(planState)
                            ? ClientStatus.PLAN_TO_REVIEW
                            : ClientStatus.NO_PLAN,
                    Set.of());
        }
        Set<StatusReason> reasons = EnumSet.noneOf(StatusReason.class);
        if (completion != null && completion.below(WATCH_BELOW)) {
            reasons.add(StatusReason.LOW_COMPLETION);
        }
        if (averageRpe != null && averageRpe.compareTo(HIGH_RPE_ABOVE) > 0) {
            reasons.add(StatusReason.HIGH_RPE);
        }
        if (completion != null && completion.below(AT_RISK_BELOW)) {
            return new Verdict(ClientStatus.AT_RISK, reasons);
        }
        return new Verdict(reasons.isEmpty() ? ClientStatus.ON_TRACK : ClientStatus.WATCH, reasons);
    }
}
