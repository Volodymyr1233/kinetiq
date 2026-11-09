package com.kinetiq.planchange.domain.rule;

import com.kinetiq.planchange.domain.model.Finding;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.WeekTraining;
import java.util.List;
import java.util.Optional;

/** A pure decision over completed weeks of one client; it reads nothing and changes nothing. */
public interface PlanChangeRule {

    Rule rule();

    /** The finding when the rule fires. {@code window} is exactly {@code rule().weeks()} weeks, oldest first. */
    Optional<Finding> evaluate(List<WeekTraining> window);
}
