package com.kinetiq.overview.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.overview.domain.input.Overview;
import com.kinetiq.overview.domain.input.Overview.Alert;
import com.kinetiq.overview.domain.input.Overview.Kind;
import com.kinetiq.overview.domain.input.Overview.OpenPlanChange;
import com.kinetiq.overview.domain.input.Overview.Row;
import com.kinetiq.overview.domain.input.Overview.Tiles;
import com.kinetiq.overview.domain.input.Overview.Waiting;
import com.kinetiq.overview.domain.input.ViewOverviewUseCase;
import com.kinetiq.overview.domain.model.ClientStatus;
import com.kinetiq.overview.domain.model.ClientTraining;
import com.kinetiq.overview.domain.model.ClientTraining.DayCheckIn;
import com.kinetiq.overview.domain.model.ClientTraining.LastLog;
import com.kinetiq.overview.domain.model.ClientTraining.WeekSessions;
import com.kinetiq.overview.domain.model.Completion;
import com.kinetiq.overview.domain.model.PlanDates;
import com.kinetiq.overview.domain.model.RpeTotal;
import com.kinetiq.overview.domain.model.Verdict;
import com.kinetiq.overview.domain.output.TrainingRecords;
import com.kinetiq.plan.domain.input.ListDraftedPlansUseCase;
import com.kinetiq.plan.domain.model.DraftedPlan;
import com.kinetiq.planchange.domain.input.PlanChangeApprovalView;
import com.kinetiq.planchange.domain.input.ViewPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.readiness.domain.model.Readiness;
import com.kinetiq.readiness.domain.model.TargetChoice;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OverviewService implements ViewOverviewUseCase {

    private static final int RPE_DAYS = 14;
    private static final int DAYS_WITHOUT_LOG = 6;
    // a client's today is at most a day from the UTC date, and the completion looks 27 days further back
    private static final int DAYS_READ = 29;
    private static final ClientTraining NOTHING = new ClientTraining(null, Map.of(), null, Map.of());

    private final ViewClientsUseCase clients;
    private final ListDraftedPlansUseCase draftedPlans;
    private final ViewPlanChangeUseCase planChanges;
    private final TrainingRecords trainingRecords;
    private final Clock clock;

    @Override
    public Overview overview(AccountId trainer) {
        Instant now = clock.instant();
        Map<AccountId, ClientTraining> records =
                trainingRecords.forTrainer(trainer, LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(DAYS_READ));
        Map<AccountId, DraftedPlan> drafts = draftedPlans.draftedPlans(trainer).stream()
                .collect(Collectors.toMap(DraftedPlan::client, Function.identity()));
        List<PlanChangeApprovalView> open = planChanges.approvals(trainer).stream()
                .filter(view -> view.planChange().isOpen())
                .toList();
        // at most one plan change per client is open
        Map<AccountId, OpenPlanChange> openByClient = open.stream()
                .collect(Collectors.toMap(view -> view.planChange().getClient(), OverviewService::open));
        List<Row> rows = new ArrayList<>();
        List<Alert> alerts = new ArrayList<>();
        int active = 0;
        WeekSessions sessions = new WeekSessions(0, 0);
        RpeTotal thisWeek = RpeTotal.NONE;
        RpeTotal lastWeek = RpeTotal.NONE;
        for (ClientSummary client : clients.listClients(trainer)) {
            ClientTraining training = records.getOrDefault(client.getId(), NOTHING);
            LocalDate today = now.atZone(client.getTimeZone()).toLocalDate();
            Row row = row(client, training, today, drafts.containsKey(client.getId()));
            rows.add(row);
            alerts.addAll(alerts(row, training, today, drafts.get(client.getId()), openByClient.get(client.getId())));
            if (training.running(today)) {
                active++;
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                WeekSessions week = training.week(monday);
                sessions = new WeekSessions(sessions.planned() + week.planned(), sessions.done() + week.done());
                thisWeek = thisWeek.plus(training.rpe(monday, monday.plusDays(6)));
                lastWeek = lastWeek.plus(training.rpe(monday.minusWeeks(1), monday.minusDays(1)));
            }
        }
        // a stable sort keeps the clients' name order within each kind
        alerts.sort(Comparator.comparing(Alert::kind));
        BigDecimal averageRpe = thisWeek.mean().orElse(null);
        return new Overview(
                new Tiles(
                        active,
                        rows.size(),
                        sessions.done(),
                        sessions.planned(),
                        averageRpe,
                        averageRpe == null
                                ? null
                                : lastWeek.mean().map(averageRpe::subtract).orElse(null),
                        drafts.size(),
                        (int) open.stream()
                                .filter(view -> !view.planChange().acceptedBy(Side.TRAINER))
                                .count()),
                alerts,
                rows);
    }

    private static Row row(ClientSummary client, ClientTraining training, LocalDate today, boolean draftWaiting) {
        boolean running = training.running(today);
        Completion completion = training.completion(today).orElse(null);
        BigDecimal averageRpe =
                training.rpe(today.minusDays(RPE_DAYS - 1L), today).mean().orElse(null);
        DayCheckIn checkIn = training.checkIns().get(today);
        Readiness readiness = checkIn == null ? null : Readiness.of(checkIn.answers());
        return new Row(
                client,
                training.plan(),
                running ? training.plan().week(today) : null,
                Verdict.of(client.getPlanState(), draftWaiting, running, completion, averageRpe),
                completion,
                averageRpe,
                training.lastLog() == null ? null : training.lastLog().at(),
                readiness,
                readiness != null
                        && readiness.band() == Readiness.Band.RECOVERY_DAY
                        && checkIn.targets() == TargetChoice.LIGHTER);
    }

    private static List<Alert> alerts(
            Row row, ClientTraining training, LocalDate today, DraftedPlan draft, OpenPlanChange planChange) {
        AccountId client = row.client().getId();
        List<Alert> alerts = new ArrayList<>();
        ClientStatus status = row.verdict().status();
        if (status == ClientStatus.AT_RISK || status == ClientStatus.WATCH) {
            alerts.add(new Alert(
                    status == ClientStatus.AT_RISK ? Kind.AT_RISK : Kind.WATCH,
                    client,
                    planChange,
                    null,
                    null,
                    null,
                    null));
        } else if (planChange != null) {
            alerts.add(new Alert(Kind.PLAN_CHANGE, client, planChange, null, null, null, null));
        }
        PlanDates plan = row.plan();
        if (training.running(today)) {
            LastLog last = training.lastLog();
            LocalDate since = last == null ? plan.publishedOn() : last.date();
            long days = ChronoUnit.DAYS.between(since, today);
            if (days >= DAYS_WITHOUT_LOG) {
                alerts.add(new Alert(
                        Kind.NO_LOG, client, null, null, (int) days, since, last == null ? null : last.sessionName()));
            }
        }
        if (draft != null) {
            alerts.add(new Alert(Kind.DRAFT, client, null, draft.kind(), null, null, null));
        }
        if (plan != null && (plan.finished(today) || plan.week(today) == plan.weeks())) {
            alerts.add(Alert.of(Kind.PLAN_ENDING, client));
        }
        if (row.readiness() != null && row.readiness().band() == Readiness.Band.RECOVERY_DAY) {
            alerts.add(Alert.of(Kind.RECOVERY_DAY, client));
        }
        return alerts;
    }

    private static OpenPlanChange open(PlanChangeApprovalView view) {
        PlanChange change = view.planChange();
        Waiting waiting;
        if (change.acceptedBy(Side.TRAINER)) {
            waiting = Waiting.CLIENT;
        } else if (change.acceptedBy(Side.CLIENT)) {
            waiting = Waiting.TRAINER;
        } else {
            waiting = Waiting.BOTH;
        }
        return new OpenPlanChange(change.getId(), view.text().title(), waiting);
    }
}
