package com.kinetiq.overview.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;

/**
 * The dates of a client's published plan: the Monday of its first week, the day it was published, its number of
 * weeks and the weekdays of its sessions, as the plan is when read.
 */
public record PlanDates(LocalDate firstWeek, LocalDate publishedOn, int weeks, Set<DayOfWeek> days) {

    private static final int DAYS_PER_WEEK = 7;

    public PlanDates {
        Objects.requireNonNull(firstWeek);
        Objects.requireNonNull(publishedOn);
        if (weeks < 1) {
            throw new IllegalArgumentException("a plan has at least one week");
        }
        days = Set.copyOf(days);
    }

    /** The Sunday of the plan's last week. */
    public LocalDate lastDay() {
        return firstWeek.plusWeeks(weeks).minusDays(1);
    }

    /** The plan week {@code today} falls in, counted from 1; above {@code weeks} once the plan has finished. */
    public int week(LocalDate today) {
        return (int) Math.floorDiv(ChronoUnit.DAYS.between(firstWeek, today), DAYS_PER_WEEK) + 1;
    }

    public boolean finished(LocalDate today) {
        return today.isAfter(lastDay());
    }

    /** Whether the plan has a session on {@code date}: its weekday, from the publish day to the plan's last day. */
    public boolean planned(LocalDate date) {
        return days.contains(date.getDayOfWeek()) && !date.isBefore(publishedOn) && !date.isAfter(lastDay());
    }
}
