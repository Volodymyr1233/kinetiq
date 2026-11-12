package com.kinetiq.overview.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.overview.domain.model.Completion;
import com.kinetiq.overview.domain.model.PlanDates;
import com.kinetiq.overview.domain.model.Verdict;
import com.kinetiq.plan.domain.model.DraftedPlan;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.readiness.domain.model.Readiness;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** What a trainer reads of all their clients at once: the tiles, the alerts, most severe first, and a row per client. */
public record Overview(Tiles tiles, List<Alert> alerts, List<Row> rows) {

    public Overview {
        Objects.requireNonNull(tiles);
        alerts = List.copyOf(alerts);
        rows = List.copyOf(rows);
    }

    /**
     * The figures across the active clients: how many there are of all clients, the sessions planned and done in
     * their current weeks, the mean RPE of this week's sets and its change against last week's ({@code null} when
     * either week has no set), and the drafts and plan changes waiting for the trainer.
     */
    public record Tiles(
            int activeClients,
            int totalClients,
            int sessionsDone,
            int sessionsPlanned,
            BigDecimal averageRpe,
            BigDecimal averageRpeChange,
            int draftsWaiting,
            int planChangesWaiting) {}

    /**
     * One client. {@code plan} is {@code null} without a published plan and {@code week} without a running one;
     * {@code completion} covers the last 28 days and {@code averageRpe} the last 14, each {@code null} without a due
     * session or a set; {@code lastLoggedAt} is {@code null} before the first set and {@code readiness} without a
     * check-in today.
     */
    public record Row(
            ClientSummary client,
            PlanDates plan,
            Integer week,
            Verdict verdict,
            Completion completion,
            BigDecimal averageRpe,
            Instant lastLoggedAt,
            Readiness readiness,
            boolean lighterTargets) {

        public Row {
            Objects.requireNonNull(client);
            Objects.requireNonNull(verdict);
        }
    }

    /** What an alert is about, declared most severe first, the order of the list. */
    public enum Kind {
        AT_RISK,
        WATCH,
        NO_LOG,
        PLAN_CHANGE,
        DRAFT,
        PLAN_ENDING,
        RECOVERY_DAY
    }

    /** Whose acceptance an open plan change waits for. */
    public enum Waiting {
        TRAINER,
        CLIENT,
        BOTH
    }

    /** An open plan change, with its title in the trainer's language. */
    public record OpenPlanChange(PlanChangeId id, String title, Waiting waitingFor) {

        public OpenPlanChange {
            Objects.requireNonNull(id);
            Objects.requireNonNull(title);
            Objects.requireNonNull(waitingFor);
        }
    }

    /**
     * One thing that asks for the trainer about one client. {@code planChange} is the client's open plan change for
     * At risk, Watch and plan change alerts, when there is one; {@code draft} is set for a draft; a no-log alert has
     * {@code daysWithoutLog}, {@code since} (the last session's date, else the publish day) and {@code lastSessionName}
     * when anything was logged. Every other field is {@code null}.
     */
    public record Alert(
            Kind kind,
            AccountId client,
            OpenPlanChange planChange,
            DraftedPlan.Kind draft,
            Integer daysWithoutLog,
            LocalDate since,
            String lastSessionName) {

        public Alert {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(client);
        }

        /** An alert that needs nothing but its kind and client. */
        public static Alert of(Kind kind, AccountId client) {
            return new Alert(kind, client, null, null, null, null, null);
        }
    }
}
