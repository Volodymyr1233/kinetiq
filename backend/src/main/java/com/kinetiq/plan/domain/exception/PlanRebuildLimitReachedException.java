package com.kinetiq.plan.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;
import java.time.Instant;
import java.util.Map;

/** The trainer has used the day's automatic builds; {@code availableAt} is when the next one is allowed. */
public class PlanRebuildLimitReachedException extends DomainException {

    private final Instant availableAt;

    public PlanRebuildLimitReachedException(Instant availableAt) {
        super("the trainer has used the day's automatic builds");
        this.availableAt = availableAt;
    }

    @Override
    public String code() {
        return "plan.rebuild-limit-reached";
    }

    @Override
    public Kind kind() {
        return Kind.TOO_MANY_ATTEMPTS;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("availableAt", availableAt.toString());
    }
}
