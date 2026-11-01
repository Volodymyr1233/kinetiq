package com.kinetiq.plan.domain.model;

import com.kinetiq.plan.domain.exception.PlanRuleViolationException;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** A named training occasion on one day of the week, with its exercises in order. The name is trimmed. */
public record Session(String name, DayOfWeek day, List<ExerciseEntry> entries) {

    public static final int NAME_MAX_LENGTH = 60;
    public static final int MAX_EXERCISES = 30;

    // also trims no-break and other Unicode spaces, which strip() keeps, as the editor's trim() does
    private static final Pattern EDGE_SPACES = Pattern.compile("^[\\s\\p{Z}]+|[\\s\\p{Z}]+$");

    public Session {
        Objects.requireNonNull(name);
        Objects.requireNonNull(day);
        name = EDGE_SPACES.matcher(name).replaceAll("");
        if (name.isEmpty() || name.length() > NAME_MAX_LENGTH) {
            throw new PlanRuleViolationException("plan.session-name-invalid");
        }
        if (entries.size() > MAX_EXERCISES) {
            throw new PlanRuleViolationException("plan.too-many-exercises");
        }
        entries = List.copyOf(entries);
    }
}
