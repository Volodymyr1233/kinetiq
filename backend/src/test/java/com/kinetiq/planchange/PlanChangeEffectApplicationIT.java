package com.kinetiq.planchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.planchange.domain.input.ApplyPlanChangesUseCase;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * The whole application against a real database and the real clock: plan changes whose effective Monday is in the
 * past are settled by the use case that the sweep calls, and the plan, its revision, the plan change and the API
 * answers are read back. Effective weeks are past Mondays, so no clock has to be set.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "kinetiq.plan-drafting.api-key=")
@AutoConfigureMockMvc
class PlanChangeEffectApplicationIT {

    private static final String SQUAT = "00000000-0000-4000-9100-000000000011";
    private static final String PLANK = "00000000-0000-4000-9100-000000000026";
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final LocalDate THIS_MONDAY =
            LocalDate.now(WARSAW).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    private static final LocalDate FIRST_WEEK = THIS_MONDAY.minusWeeks(4);
    private static final LocalDate EFFECTIVE_WEEK = THIS_MONDAY.minusWeeks(1);

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ApplyPlanChangesUseCase settlement;

    @Autowired
    private MockMvcTester mvc;

    private final List<UUID> accounts = new java.util.ArrayList<>();

    @AfterEach
    void cleanUp() {
        // clients first: the trainer owns them
        accounts.reversed().forEach(id -> jdbc.sql("delete from accounts where id = ?").param(id).update());
    }

    private UUID account(String role, UUID trainer) {
        UUID id = UUID.randomUUID();
        jdbc.sql(
                        """
                        insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone,
                                              email_confirmed, health_data_consent_at, created_at, updated_at, version)
                        values (?, 'Person', ?, 'hash', ?, ?, 'Europe/Warsaw', true,
                                case when ? = 'CLIENT' then now() end, now(), now(), 0)
                        """)
                .params(id, id + "@example.com", role, trainer, role)
                .update();
        accounts.add(id);
        return id;
    }

    // a published plan from FIRST_WEEK with the squat on Monday at 10 repetitions and a plank on Thursday
    private UUID plan(UUID client, int squatAmount) {
        UUID plan = UUID.randomUUID();
        jdbc.sql(
                        """
                        insert into plans (id, client_id, goal, weeks, first_week, published_on, client_seen_at,
                                           created_at, updated_at, version)
                        values (?, ?, 'BUILD_MUSCLE', 12, ?, ?, now(), now(), now(), 0)
                        """)
                .params(plan, client, FIRST_WEEK, FIRST_WEEK)
                .update();
        session(plan, "MONDAY", SQUAT, squatAmount, "60.0");
        session(plan, "THURSDAY", PLANK, 30, null);
        return plan;
    }

    private void session(UUID plan, String day, String exercise, int amount, String load) {
        UUID session = UUID.randomUUID();
        jdbc.sql("insert into plan_sessions (id, plan_id, name, day_of_week) values (?, ?, ?, ?)")
                .params(session, plan, "Session " + day, day)
                .update();
        jdbc.sql(
                        """
                        insert into plan_session_entries (session_id, position, exercise_id, sets, amount, load_kg,
                                                          rpe, rest_seconds)
                        values (?, 0, ?::uuid, 3, ?, ?::numeric, 8, 90)
                        """)
                .params(session, exercise, amount, load)
                .update();
    }

    // the squat of Monday goes from 10 to 9 repetitions; accepted by both when {@code accepted}
    private UUID planChange(UUID client, boolean trainerAccepted, boolean clientAccepted) {
        UUID id = UUID.randomUUID();
        String state = trainerAccepted && clientAccepted ? "APPLIED" : trainerAccepted || clientAccepted ? "ACCEPTED_BY_ONE" : "PENDING";
        jdbc.sql(
                        """
                        insert into plan_changes (id, client_id, rule, state, plan_version, window_start, window_end,
                                                  effective_week, created_at, ended_at, trainer_accepted_at,
                                                  client_accepted_at)
                        values (?, ?, 'HIGH_RPE', ?, 0, ?, ?, ?, now() - interval '9 days',
                                case when ? = 'APPLIED' then now() - interval '8 days' end,
                                case when ? then now() - interval '8 days' end,
                                case when ? then now() - interval '8 days' end)
                        """)
                .params(
                        id,
                        client,
                        state,
                        EFFECTIVE_WEEK.minusWeeks(2),
                        EFFECTIVE_WEEK.minusDays(1),
                        EFFECTIVE_WEEK,
                        state,
                        trainerAccepted,
                        clientAccepted)
                .update();
        jdbc.sql(
                        """
                        insert into plan_change_targets (plan_change_id, session_day, position, target, exercise_id,
                                                         session_name, old_value, new_value)
                        values (?, 'MONDAY', 0, 'AMOUNT', ?::uuid, 'Session MONDAY', 10.0, 9.0)
                        """)
                .params(id, SQUAT)
                .update();
        return id;
    }

    private Map<String, Object> row(UUID planChange) {
        return jdbc.sql("select * from plan_changes where id = ?").param(planChange).query().singleRow();
    }

    private int revisions(UUID plan) {
        return jdbc.sql("select count(*) from plan_revisions where plan_id = ?")
                .param(plan)
                .query(Integer.class)
                .single();
    }

    private int squatAmount(UUID plan) {
        return jdbc.sql(
                        """
                        select e.amount from plan_session_entries e join plan_sessions s on s.id = e.session_id
                        where s.plan_id = ? and s.day_of_week = 'MONDAY'
                        """)
                .param(plan)
                .query(Integer.class)
                .single();
    }

    private static RequestPostProcessor signedIn(UUID account, String role) {
        return user(account.toString()).roles(role);
    }

    @Test
    void shouldApplyAcceptedPlanChange_keepPastWeeksAndAnswerOnBothSides() {
        UUID trainer = account("TRAINER", null);
        UUID client = account("CLIENT", trainer);
        UUID plan = plan(client, 10);
        UUID change = planChange(client, true, true);
        long versionBefore = jdbc.sql("select version from plans where id = ?").param(plan).query(Long.class).single();

        assertThat(settlement.due()).contains(new PlanChangeId(change));
        settlement.settle(new PlanChangeId(change));

        Map<String, Object> applied = row(change);
        assertThat(applied).containsEntry("state", "APPLIED");
        assertThat(applied.get("took_effect_at")).isNotNull();
        assertThat(revisions(plan)).isOne();
        assertThat(squatAmount(plan)).isEqualTo(9);
        assertThat(jdbc.sql("select version from plans where id = ?").param(plan).query(Long.class).single())
                .isGreaterThan(versionBefore);
        assertThat(jdbc.sql(
                                """
                                select e.amount from plan_revisions r
                                join plan_revision_sessions s on s.revision_id = r.id
                                join plan_revision_session_entries e on e.session_id = s.id
                                where r.plan_id = ? and s.day_of_week = 'MONDAY'
                                """)
                        .param(plan)
                        .query(Integer.class)
                        .single())
                .isEqualTo(10);

        // a second run changes nothing
        settlement.settle(new PlanChangeId(change));
        assertThat(revisions(plan)).isOne();
        assertThat(squatAmount(plan)).isEqualTo(9);
        assertThat(settlement.due()).doesNotContain(new PlanChangeId(change));

        // the weeks before the effective Monday read the plan as it was, from it the new one
        int effectiveWeekNumber = 4;
        assertThat(mvc.get().uri("/api/v1/plan?week=" + (effectiveWeekNumber - 1)).with(signedIn(client, "CLIENT")))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.sessions[0].entries[0].amount")
                .isEqualTo(10);
        assertThat(mvc.get().uri("/api/v1/plan?week=" + effectiveWeekNumber).with(signedIn(client, "CLIENT")))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.sessions[0].entries[0].amount")
                .isEqualTo(9);

        var appliedForClient = mvc.get().uri("/api/v1/plan/changes/applied").with(signedIn(client, "CLIENT")).exchange();
        assertThat(appliedForClient).hasStatus(HttpStatus.OK);
        assertThat(appliedForClient).bodyJson().extractingPath("$.tookEffectAt").isNotNull();
        assertThat(mvc.get().uri("/api/v1/plan/changes/upcoming").with(signedIn(client, "CLIENT")))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("plan-change.not-found");
        assertThat(mvc.get().uri("/api/v1/clients/" + client + "/plan-changes/ended").with(signedIn(trainer, "TRAINER")))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.items[0].tookEffectAt")
                .isNotNull();
    }

    @Test
    void shouldExpireUndecidedPlanChanges_andLeaveThePlanAlone() {
        UUID trainer = account("TRAINER", null);
        UUID pendingClient = account("CLIENT", trainer);
        UUID pendingPlan = plan(pendingClient, 10);
        UUID pending = planChange(pendingClient, false, false);
        UUID oneClient = account("CLIENT", trainer);
        UUID onePlan = plan(oneClient, 10);
        UUID acceptedByOne = planChange(oneClient, true, false);

        settlement.settle(new PlanChangeId(pending));
        settlement.settle(new PlanChangeId(acceptedByOne));

        for (UUID expired : List.of(pending, acceptedByOne)) {
            assertThat(row(expired)).containsEntry("state", "EXPIRED").containsEntry("expiry_cause", "UNDECIDED");
            assertThat(row(expired).get("took_effect_at")).isNull();
        }
        assertThat(squatAmount(pendingPlan)).isEqualTo(10);
        assertThat(squatAmount(onePlan)).isEqualTo(10);
        assertThat(revisions(pendingPlan) + revisions(onePlan)).isZero();
    }

    @Test
    void shouldExpireAsPlanEdited_whenTheTrainerEditedTheTouchedValue() {
        UUID trainer = account("TRAINER", null);
        UUID client = account("CLIENT", trainer);
        UUID plan = plan(client, 12);
        UUID change = planChange(client, true, true);

        settlement.settle(new PlanChangeId(change));

        assertThat(row(change)).containsEntry("state", "EXPIRED").containsEntry("expiry_cause", "PLAN_EDITED");
        assertThat(squatAmount(plan)).isEqualTo(12);
        assertThat(revisions(plan)).isZero();
        assertThat(mvc.get().uri("/api/v1/plan/changes/" + change).with(signedIn(client, "CLIENT")))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.expiryCause")
                .isEqualTo("PLAN_EDITED");
    }

    @Test
    void shouldAnswerNotFoundOrRefuse_forEveryoneElseOnTheNewReads() {
        UUID trainer = account("TRAINER", null);
        UUID client = account("CLIENT", trainer);
        plan(client, 10);
        UUID change = planChange(client, true, true);
        settlement.settle(new PlanChangeId(change));
        UUID otherTrainer = account("TRAINER", null);
        UUID otherClient = account("CLIENT", otherTrainer);

        // another trainer asks for the client of the first, another client for the plan change of the first
        assertThat(mvc.get().uri("/api/v1/clients/" + client + "/plan-changes/upcoming").with(signedIn(otherTrainer, "TRAINER")))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("client.not-found");
        assertThat(mvc.get().uri("/api/v1/clients/" + client + "/plan-changes/ended").with(signedIn(otherTrainer, "TRAINER")))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/v1/plan-changes/" + change).with(signedIn(otherTrainer, "TRAINER")))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/v1/plan/changes/" + change).with(signedIn(otherClient, "CLIENT")))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/v1/plan/changes/applied").with(signedIn(otherClient, "CLIENT")))
                .hasStatus(HttpStatus.NOT_FOUND);
        // roles and anonymous callers
        assertThat(mvc.get().uri("/api/v1/plan/changes/applied").with(signedIn(trainer, "TRAINER")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/v1/clients/" + client + "/plan-changes/upcoming").with(signedIn(client, "CLIENT")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/v1/plan/changes/upcoming")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/v1/clients/" + client + "/plan-changes/upcoming"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
