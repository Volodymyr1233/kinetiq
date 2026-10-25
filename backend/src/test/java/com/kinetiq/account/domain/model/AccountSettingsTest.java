package com.kinetiq.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AccountSettingsTest {

    private static final AccountSettings STORED = new AccountSettings(WeightUnit.LB, Language.PL, Theme.DARK);

    @Nested
    class StartingWith {

        @Test
        void shouldStartWithKilogramsAndDeviceTheme_whenGivenALanguage() {
            assertThat(AccountSettings.startingWith(Language.PL))
                    .isEqualTo(new AccountSettings(WeightUnit.KG, Language.PL, Theme.SYSTEM));
        }
    }

    @Nested
    class With {

        @Test
        void shouldChangeOnlyTheGivenValues_whenOthersAreNull() {
            assertThat(STORED.with(null, Language.EN, null))
                    .isEqualTo(new AccountSettings(WeightUnit.LB, Language.EN, Theme.DARK));
        }

        @Test
        void shouldKeepEverything_whenAllAreNull() {
            assertThat(STORED.with(null, null, null)).isEqualTo(STORED);
        }
    }

    @Test
    void shouldRefuse_whenAValueIsMissing() {
        assertThatThrownBy(() -> new AccountSettings(WeightUnit.KG, null, Theme.SYSTEM))
                .isInstanceOf(NullPointerException.class);
    }
}
