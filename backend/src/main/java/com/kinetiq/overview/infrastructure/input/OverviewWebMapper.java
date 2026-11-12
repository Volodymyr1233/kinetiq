package com.kinetiq.overview.infrastructure.input;

import com.kinetiq.overview.domain.input.Overview;
import com.kinetiq.overview.domain.input.Overview.Alert;
import com.kinetiq.overview.domain.input.Overview.Row;
import com.kinetiq.overview.domain.model.Completion;
import com.kinetiq.overview.domain.model.PlanDates;
import com.kinetiq.overview.infrastructure.input.OverviewResponse.OverviewAlertResponse;
import com.kinetiq.overview.infrastructure.input.OverviewResponse.OverviewClientResponse;
import com.kinetiq.readiness.domain.model.Readiness;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class OverviewWebMapper {

    OverviewResponse toResponse(Overview overview) {
        Overview.Tiles tiles = overview.tiles();
        return new OverviewResponse(
                tiles.activeClients(),
                tiles.totalClients(),
                tiles.sessionsDone(),
                tiles.sessionsPlanned(),
                tiles.averageRpe(),
                tiles.averageRpeChange(),
                tiles.draftsWaiting(),
                tiles.planChangesWaiting(),
                overview.alerts().stream().map(OverviewWebMapper::alert).toList(),
                overview.rows().stream().map(OverviewWebMapper::client).toList());
    }

    private static OverviewAlertResponse alert(Alert alert) {
        Optional<Overview.OpenPlanChange> planChange = Optional.ofNullable(alert.planChange());
        return new OverviewAlertResponse(
                alert.kind(),
                alert.client().value(),
                planChange.map(change -> change.id().value()).orElse(null),
                planChange.map(Overview.OpenPlanChange::title).orElse(null),
                planChange.map(Overview.OpenPlanChange::waitingFor).orElse(null),
                alert.draft(),
                alert.daysWithoutLog(),
                alert.since(),
                alert.lastSessionName());
    }

    // reasons in their declared order, so a list of them reads the same every time
    private static OverviewClientResponse client(Row row) {
        var plan = Optional.ofNullable(row.plan());
        var completion = Optional.ofNullable(row.completion());
        var readiness = Optional.ofNullable(row.readiness());
        return new OverviewClientResponse(
                row.client().getId().value(),
                row.client().getName(),
                row.client().getGoal().orElse(null),
                row.client().getPlanState(),
                row.week(),
                plan.map(PlanDates::weeks).orElse(null),
                plan.map(PlanDates::lastDay).orElse(null),
                row.verdict().status(),
                row.verdict().reasons().stream().sorted().toList(),
                completion.map(Completion::done).orElse(null),
                completion.map(Completion::due).orElse(null),
                row.averageRpe(),
                row.lastLoggedAt(),
                readiness.map(Readiness::score).orElse(null),
                readiness.map(Readiness::band).orElse(null),
                row.lighterTargets());
    }
}
