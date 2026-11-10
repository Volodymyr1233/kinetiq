package com.kinetiq.planchange.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * What goes to the drafting service for one attempt at plan change content. {@code rejections} holds a sentence for
 * each reason the previous answer was refused, and is empty on a first attempt.
 */
public record PlanChangePromptMessages(String system, String user, List<String> rejections) {

    public PlanChangePromptMessages {
        Objects.requireNonNull(system);
        Objects.requireNonNull(user);
        rejections = List.copyOf(rejections);
    }
}
