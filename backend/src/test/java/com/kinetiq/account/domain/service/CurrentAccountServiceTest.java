package com.kinetiq.account.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.exception.AccountNotFoundException;
import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Theme;
import com.kinetiq.account.domain.model.WeightUnit;
import com.kinetiq.common.domain.model.Email;
import java.time.ZoneId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CurrentAccountServiceTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final CurrentAccountService service = new CurrentAccountService(accounts);
    private final Account trainer = accounts.add(
            Account.signUpTrainer("Marek Nowak", new Email("marek@example.com"), "hash", WARSAW, Language.EN));

    @Nested
    class Find {

        @Test
        void shouldReturnAccount_whenItExists() {
            assertThat(service.find(trainer.getId())).isSameAs(trainer);
        }

        @Test
        void shouldCountAsSignedOut_whenAccountNoLongerExists() {
            assertThatThrownBy(() -> service.find(AccountId.random())).isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    class ChangeTimeZone {

        @Test
        void shouldStoreZone_whenItDiffers() {
            service.changeTimeZone(trainer.getId(), ZoneId.of("Europe/London"));

            assertThat(trainer.getTimeZone()).isEqualTo(ZoneId.of("Europe/London"));
            assertThat(accounts.saves).isOne();
        }

        @Test
        void shouldSaveNothing_whenZoneIsTheSame() {
            service.changeTimeZone(trainer.getId(), WARSAW);

            assertThat(accounts.saves).isZero();
        }
    }

    @Nested
    class Update {

        @Test
        void shouldChangeOnlyTheValuesSent_whenOthersAreMissing() {
            Account updated =
                    service.update(trainer.getId(), new UpdateAccountCommand(null, WeightUnit.LB, null, Theme.DARK));

            assertThat(updated.getSettings()).isEqualTo(new AccountSettings(WeightUnit.LB, Language.EN, Theme.DARK));
            assertThat(updated.getName()).isEqualTo("Marek Nowak");
            assertThat(accounts.saves).isOne();
        }

        @Test
        void shouldRenameAndKeepSettings_whenOnlyANameIsSent() {
            AccountSettings before = trainer.getSettings();

            service.update(trainer.getId(), new UpdateAccountCommand("Marek Kowalski", null, null, null));

            assertThat(trainer.getName()).isEqualTo("Marek Kowalski");
            assertThat(trainer.getSettings()).isEqualTo(before);
        }

        @Test
        void shouldKeepSettings_whenNothingIsSent() {
            AccountSettings before = trainer.getSettings();

            service.update(trainer.getId(), new UpdateAccountCommand(null, null, null, null));

            assertThat(trainer.getSettings()).isEqualTo(before);
        }

        @Test
        void shouldCountAsSignedOut_whenAccountNoLongerExists() {
            UpdateAccountCommand command = new UpdateAccountCommand(null, null, Language.PL, null);

            assertThatThrownBy(() -> service.update(AccountId.random(), command))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }
}
