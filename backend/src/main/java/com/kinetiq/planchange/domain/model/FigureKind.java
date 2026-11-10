package com.kinetiq.planchange.domain.model;

/** What a figure measures. Only {@code BEST_LOAD_KG} belongs to one exercise. */
public enum FigureKind {
    MEAN_RPE,
    BEST_LOAD_KG,
    SESSIONS_PLANNED,
    SESSIONS_FINISHED
}
