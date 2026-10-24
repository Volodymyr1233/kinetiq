package com.kinetiq.account.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.exception.AccountNotFoundException;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Email;
import java.time.ZoneId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CurrentAccountServiceTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final CurrentAccountService service = new CurrentAccountService(accounts);
    private final Account trainer =
            accounts.add(Account.signUpTrainer("Marek Nowak", new Email("marek@example.com"), "hash", WARSAW));

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
}
