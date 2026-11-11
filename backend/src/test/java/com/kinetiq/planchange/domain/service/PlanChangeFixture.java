package com.kinetiq.planchange.domain.service;

import static java.time.DayOfWeek.MONDAY;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import com.kinetiq.quiz.domain.model.Goal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A trainer with two clients (one using Polish), another trainer with a client, the accounts and clients ports over
 * them, and a pending plan change for a client that takes effect on Monday 16 November 2026. The clock starts on the
 * Thursday before.
 */
class PlanChangeFixture {

    static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    static final ExerciseId SQUAT = ExerciseId.random();

    final MutableClock clock = new MutableClock("2026-11-12T10:00:00Z");
    final FakePlanChangeRepository planChanges = new FakePlanChangeRepository();
    final FakePlanSource plans = new FakePlanSource();
    final Map<AccountId, Account> accounts = new HashMap<>();
    final FakeExchangeMessages messages = new FakeExchangeMessages();

    final AccountId trainer = account("Marek Trainer", Role.TRAINER, null, Language.EN);
    final AccountId otherTrainer = account("Ina Other", Role.TRAINER, null, Language.EN);
    final AccountId client = account("Ola Nowak", Role.CLIENT, trainer, Language.EN);
    final AccountId polishClient = account("Ewa Kowal", Role.CLIENT, trainer, Language.PL);
    final AccountId otherClient = account("Jan Obcy", Role.CLIENT, otherTrainer, Language.EN);
    final AccountId clientWithoutTrainer = account("Kasia Sama", Role.CLIENT, null, Language.EN);

    // a trainer reads only their own clients
    final ViewClientsUseCase clients = new ViewClientsUseCase() {
        @Override
        public List<ClientSummary> listClients(AccountId asker) {
            return List.of();
        }

        @Override
        public ClientSummary getClient(AccountId asker, AccountId id) {
            Account found = accounts.get(id);
            if (found == null || found.getTrainerId().filter(asker::equals).isEmpty()) {
                throw new ClientNotFoundException();
            }
            return new ClientSummary(id, found.getName(), Goal.LOSE_FAT, PlanState.PUBLISHED, found.getTimeZone());
        }
    };

    final CurrentAccountUseCase accountsPort = new CurrentAccountUseCase() {
        @Override
        public Account find(AccountId id) {
            return accounts.get(id);
        }

        @Override
        public void changeTimeZone(AccountId id, ZoneId zone) {}

        @Override
        public Account update(AccountId id, UpdateAccountCommand command) {
            return find(id);
        }
    };

    PlanChangeFixture() {
        plans.names.put(SQUAT, "Back squat");
    }

    private AccountId account(String name, Role role, AccountId trainerId, Language language) {
        AccountId id = AccountId.random();
        accounts.put(
                id,
                Account.restore(
                        id,
                        name,
                        new Email(name.split(" ")[0].toLowerCase() + "@example.com"),
                        "hash",
                        role,
                        trainerId,
                        WARSAW,
                        AccountSettings.startingWith(language),
                        true,
                        null,
                        null,
                        null,
                        null));
        return id;
    }

    /** Stores a pending plan change of {@code forClient}: the squat loses a repetition per set. */
    PlanChange pendingFor(AccountId forClient) {
        return planChanges.save(new PlanChange(
                PlanChangeId.random(),
                forClient,
                Rule.HIGH_RPE,
                PlanChangeState.PENDING,
                1,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-08"),
                LocalDate.parse("2026-11-16"),
                PlanChangeContent.ofChanges(List.of(new TargetChange(
                        new EntryRef(MONDAY, 0), SQUAT, "Lower", Target.AMOUNT, BigDecimal.TEN, BigDecimal.valueOf(9)))),
                List.of(
                        Figure.ofWeek(FigureKind.MEAN_RPE, LocalDate.parse("2026-10-26"), new BigDecimal("8.5")),
                        Figure.ofWeek(FigureKind.MEAN_RPE, LocalDate.parse("2026-11-02"), new BigDecimal("8.7"))),
                Set.of(SQUAT),
                Instant.parse("2026-11-09T02:00:00Z"),
                null,
                null,
                null,
                null));
    }
}
