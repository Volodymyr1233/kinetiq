package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.ScheduledClient;
import com.kinetiq.planchange.domain.model.WeekTraining;
import com.kinetiq.planchange.domain.output.TrainingHistory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.IntStream;

/** Clients with a published plan from {@code 2026-09-07}, and the weeks claimed for them. */
class FakeTrainingHistory implements TrainingHistory {

    private static final LocalDate FIRST_WEEK = LocalDate.parse("2026-09-07");

    private final Map<AccountId, ZoneId> zones = new LinkedHashMap<>();
    private final Map<AccountId, TreeSet<LocalDate>> claimed = new LinkedHashMap<>();
    final List<String> claims = new ArrayList<>();
    private final Map<LocalDate, WeekTraining> trained = new LinkedHashMap<>();
    int weekReads;

    AccountId publishedPlan(String zone) {
        AccountId client = AccountId.random();
        zones.put(client, ZoneId.of(zone));
        return client;
    }

    void checked(AccountId client, String week) {
        claimed.computeIfAbsent(client, id -> new TreeSet<>()).add(LocalDate.parse(week));
    }

    void trained(WeekTraining week) {
        trained.put(week.monday(), week);
    }

    List<LocalDate> claimedWeeks(AccountId client) {
        return List.copyOf(claimed.getOrDefault(client, new TreeSet<>()));
    }

    @Override
    public List<ScheduledClient> clientsWithPublishedPlan() {
        return zones.keySet().stream().map(this::scheduled).toList();
    }

    @Override
    public Optional<ScheduledClient> scheduledClient(AccountId client) {
        return zones.containsKey(client) ? Optional.of(scheduled(client)) : Optional.empty();
    }

    @Override
    public List<WeekTraining> weeks(AccountId client, LocalDate firstMonday, int count) {
        weekReads++;
        return IntStream.range(0, count)
                .mapToObj(week -> {
                    LocalDate monday = firstMonday.plusWeeks(week);
                    return trained.getOrDefault(monday, new WeekTraining(monday, 3, 3, 0, List.of(), List.of()));
                })
                .toList();
    }

    @Override
    public boolean claimWeek(AccountId client, LocalDate week, Instant now) {
        claims.add(client + " " + week);
        return claimed.computeIfAbsent(client, id -> new TreeSet<>()).add(week);
    }

    private ScheduledClient scheduled(AccountId client) {
        TreeSet<LocalDate> weeks = claimed.getOrDefault(client, new TreeSet<>());
        return new ScheduledClient(
                client,
                zones.get(client),
                FIRST_WEEK,
                10,
                weeks.isEmpty() ? Optional.empty() : Optional.of(weeks.last()));
    }
}
