package com.kinetiq.planchange.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.input.ExchangeMessagesUseCase;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.planchange.domain.exception.PlanChangeEndedException;
import com.kinetiq.planchange.domain.input.DecideOwnPlanChangeUseCase;
import com.kinetiq.planchange.domain.input.DecidePlanChangeUseCase;
import com.kinetiq.planchange.domain.input.KeepCurrentPlanCommand;
import com.kinetiq.planchange.domain.model.DeclineReason;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Keeping the current plan against the whole application and a real database: the decliner's message reaches both
 * people's conversation views, once, and the decline and the message stand or fall together.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "kinetiq.plan-drafting.api-key=")
class DeclineMessageIT {

    // a starter exercise of the exercise migrations
    private static final UUID STARTER_PLANK = UUID.fromString("00000000-0000-4000-9100-000000000026");

    @Autowired
    private DecidePlanChangeUseCase trainerDecisions;

    @Autowired
    private DecideOwnPlanChangeUseCase clientDecisions;

    @Autowired
    private ExchangeMessagesUseCase messages;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private PlanChangeRepository planChanges;

    private UUID trainer;
    private UUID client;
    private UUID planChange;

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from messages where conversation_id in (select id from conversations where client_id = ?)", client);
        jdbc.update("delete from conversation_reads where conversation_id in (select id from conversations where client_id = ?)", client);
        jdbc.update("delete from conversations where client_id = ?", client);
        jdbc.update("delete from plan_change_targets where plan_change_id = ?", planChange);
        jdbc.update("delete from plan_changes where id = ?", planChange);
        jdbc.update("delete from accounts where id = ?", client);
        jdbc.update("delete from accounts where id = ?", trainer);
    }

    private void givenAPendingPlanChange() {
        trainer = UUID.randomUUID();
        client = UUID.randomUUID();
        planChange = UUID.randomUUID();
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, time_zone, email_confirmed, created_at,
                                      updated_at, version)
                values (?, 'Trainer', ?, 'hash', 'TRAINER', 'Europe/Warsaw', true, now(), now(), 0)
                """,
                trainer,
                trainer + "@example.com");
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, 'Client', ?, 'hash', 'CLIENT', ?, 'Europe/Warsaw', true, now(), now(), now(), 0)
                """,
                client,
                client + "@example.com",
                trainer);
        // next Monday, so the plan change can be decided whatever day the test runs
        jdbc.update(
                """
                insert into plan_changes (id, client_id, rule, state, plan_version, window_start, window_end,
                                          effective_week, created_at)
                values (?, ?, 'HIGH_RPE', 'PENDING', 0, (date_trunc('week', current_date) - interval '14 days')::date,
                        (date_trunc('week', current_date) - interval '1 day')::date,
                        (date_trunc('week', current_date) + interval '7 days')::date, now())
                """,
                planChange,
                client);
        jdbc.update(
                """
                insert into plan_change_targets (plan_change_id, session_day, position, target, exercise_id,
                                                 session_name, old_value, new_value)
                values (?, 'TUESDAY', 0, 'AMOUNT', ?, 'Full body', 8.0, 7.0)
                """,
                planChange,
                STARTER_PLANK);
    }

    private List<Message> conversationAs(UUID person) {
        return messages.messages(new AccountId(person), new AccountId(client), null).messages();
    }

    private String state() {
        return jdbc.queryForObject("select state from plan_changes where id = ?", String.class, planChange);
    }

    @Test
    void shouldPostOneMessageFromTheTrainer_thatBothPeopleReadAndTheClientHasNotRead() {
        givenAPendingPlanChange();

        trainerDecisions.keepCurrent(new KeepCurrentPlanCommand(
                new AccountId(trainer), new PlanChangeId(planChange), DeclineReason.SCHEDULE, "Away that week"));

        for (UUID reader : List.of(trainer, client)) {
            assertThat(conversationAs(reader)).hasSize(1).first().satisfies(message -> {
                assertThat(message.getSender()).isEqualTo(new AccountId(trainer));
                assertThat(message.getText().value())
                        .startsWith("Keeping the current plan instead of:")
                        .contains("Reason: schedule.")
                        .endsWith("\nAway that week");
            });
        }
        assertThat(messages.unreadCount(new AccountId(client))).isEqualTo(1);
        assertThat(messages.unreadCount(new AccountId(trainer))).isZero();
        assertThat(state()).isEqualTo("DECLINED");
    }

    @Test
    void shouldPostOneMessageFromTheClient_thatBothPeopleRead() {
        givenAPendingPlanChange();

        clientDecisions.keepCurrent(new KeepCurrentPlanCommand(
                new AccountId(client), new PlanChangeId(planChange), DeclineReason.INJURY, null));

        for (UUID reader : List.of(trainer, client)) {
            assertThat(conversationAs(reader)).hasSize(1).first().satisfies(message -> {
                assertThat(message.getSender()).isEqualTo(new AccountId(client));
                assertThat(message.getText().value()).contains("Reason: injury.").doesNotContain("\n");
            });
        }
        assertThat(messages.unreadCount(new AccountId(trainer))).isEqualTo(1);
        assertThat(messages.unreadCount(new AccountId(client))).isZero();
    }

    @Test
    void shouldKeepOneMessage_whenTheRequestIsRepeated() {
        givenAPendingPlanChange();
        KeepCurrentPlanCommand command = new KeepCurrentPlanCommand(
                new AccountId(client), new PlanChangeId(planChange), DeclineReason.OTHER, null);
        clientDecisions.keepCurrent(command);

        assertThatThrownBy(() -> clientDecisions.keepCurrent(command)).isInstanceOf(PlanChangeEndedException.class);
        assertThatThrownBy(() -> trainerDecisions.keepCurrent(new KeepCurrentPlanCommand(
                        new AccountId(trainer), new PlanChangeId(planChange), DeclineReason.OTHER, null)))
                .isInstanceOf(PlanChangeEndedException.class);

        assertThat(conversationAs(client)).hasSize(1);
    }

    @Test
    void shouldLeaveNeitherMessageNorDecline_whenStoringTheDeclineFailsAfterTheMessageWasSent() {
        givenAPendingPlanChange();
        doThrow(new IllegalStateException("the store fails")).when(planChanges).update(any());

        assertThatThrownBy(() -> clientDecisions.keepCurrent(new KeepCurrentPlanCommand(
                        new AccountId(client), new PlanChangeId(planChange), DeclineReason.OTHER, null)))
                .isInstanceOf(RuntimeException.class);

        assertThat(state()).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject(
                        "select count(*) from messages where conversation_id in (select id from conversations where client_id = ?)",
                        Integer.class,
                        client))
                .isZero();
    }
}
