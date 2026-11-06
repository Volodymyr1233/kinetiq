package com.kinetiq.plan.domain.model;

import java.util.List;

/**
 * What goes to the drafting service for one attempt. {@code rejections} holds a sentence for each reason the previous
 * answer was refused, and is empty on a first attempt.
 */
public record PlanPromptMessages(String system, String user, List<String> rejections) {

    public PlanPromptMessages {
        rejections = List.copyOf(rejections);
    }
}
