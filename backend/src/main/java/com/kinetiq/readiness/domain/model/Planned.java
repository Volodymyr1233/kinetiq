package com.kinetiq.readiness.domain.model;

import com.kinetiq.exercise.domain.model.Measure;
import java.util.Objects;

/** One exercise entry as planned: its targets and what its amount counts. */
public record Planned(Target target, Measure measure) {

    public Planned {
        Objects.requireNonNull(target);
        Objects.requireNonNull(measure);
    }
}
