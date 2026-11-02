package com.kinetiq.plan.infrastructure.input;

import jakarta.validation.constraints.NotNull;

/** Publishes the saved draft; {@code version} is the one this editor read. */
record PublishPlanRequest(@NotNull Long version) {}
