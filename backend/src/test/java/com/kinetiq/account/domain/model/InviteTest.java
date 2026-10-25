package com.kinetiq.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.exception.InviteNotFoundException;
import com.kinetiq.common.domain.model.Email;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InviteTest {

    private static final AccountId TRAINER = AccountId.random();
    private static final Email EMAIL = new Email("julia@example.com");
    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");
    private static final Instant EXPIRY = NOW.plus(Duration.ofDays(7));

    private static SentInvite sent() {
        return Invite.send(TRAINER, EMAIL, NOW);
    }

    private static Invite invite() {
        return sent().invite();
    }

    @Nested
    class Send {

        @Test
        void shouldOpenInviteForSevenDays_whenSent() {
            Invite invite = invite();

            assertThat(invite.getStatus()).isEqualTo(InviteStatus.OPEN);
            assertThat(invite.getSentAt()).isEqualTo(NOW);
            assertThat(invite.getExpiresAt()).isEqualTo(EXPIRY);
        }

        @Test
        void shouldStoreOnlyHashesOfTwoDifferentSecrets_whenSent() {
            SentInvite sent = sent();

            assertThat(sent.emailToken().value()).isNotEqualTo(sent.linkToken().value());
            assertThat(sent.invite().getEmailTokenHash()).isEqualTo(sent.emailToken().hash());
            assertThat(sent.invite().getLinkTokenHash()).isEqualTo(sent.linkToken().hash());
        }
    }

    @Nested
    class IsOpen {

        @Test
        void shouldBeOpen_whenOneSecondIsLeft() {
            assertThat(invite().isOpen(EXPIRY.minusSeconds(1))).isTrue();
        }

        @Test
        void shouldBeExpired_whenExactlySevenDaysPassed() {
            assertThat(invite().isOpen(EXPIRY)).isFalse();
        }
    }

    @Nested
    class Revoke {

        @Test
        void shouldRevoke_whenOpen() {
            Invite invite = invite();

            invite.revoke(NOW);

            assertThat(invite.getStatus()).isEqualTo(InviteStatus.REVOKED);
            assertThat(invite.isOpen(NOW)).isFalse();
        }

        @Test
        void shouldRefuse_whenAlreadyRevoked() {
            Invite invite = invite();
            invite.revoke(NOW);

            assertThatThrownBy(() -> invite.revoke(NOW)).isInstanceOf(InviteNotFoundException.class);
        }

        @Test
        void shouldRefuse_whenExpired() {
            Invite invite = invite();

            assertThatThrownBy(() -> invite.revoke(EXPIRY)).isInstanceOf(InviteNotFoundException.class);
            assertThat(invite.getStatus()).isEqualTo(InviteStatus.OPEN);
        }
    }

    @Nested
    class Use {

        @Test
        void shouldMarkUsed_whenOpen() {
            Invite invite = invite();

            invite.use(NOW);

            assertThat(invite.getStatus()).isEqualTo(InviteStatus.USED);
        }

        @Test
        void shouldRefuse_whenUsedBefore() {
            Invite invite = invite();
            invite.use(NOW);

            assertThatThrownBy(() -> invite.use(NOW)).isInstanceOf(InviteNotFoundException.class);
        }

        @Test
        void shouldRefuse_whenRevoked() {
            Invite invite = invite();
            invite.revoke(NOW);

            assertThatThrownBy(() -> invite.use(NOW)).isInstanceOf(InviteNotFoundException.class);
            assertThat(invite.getStatus()).isEqualTo(InviteStatus.REVOKED);
        }

        @Test
        void shouldRefuse_whenExpired() {
            assertThatThrownBy(() -> invite().use(EXPIRY)).isInstanceOf(InviteNotFoundException.class);
        }
    }

    @Nested
    class ReplaceLink {

        @Test
        void shouldReplaceOnlyTheLinkSecret_whenOpen() {
            SentInvite sent = sent();
            Invite invite = sent.invite();

            SecretToken token = invite.replaceLink(NOW);

            assertThat(invite.getLinkTokenHash())
                    .isEqualTo(token.hash())
                    .isNotEqualTo(sent.linkToken().hash());
            assertThat(invite.getEmailTokenHash()).isEqualTo(sent.emailToken().hash());
            assertThat(invite.getExpiresAt()).isEqualTo(EXPIRY);
        }

        @Test
        void shouldRefuse_whenNotOpen() {
            Invite invite = invite();
            invite.revoke(NOW);

            assertThatThrownBy(() -> invite.replaceLink(NOW)).isInstanceOf(InviteNotFoundException.class);
        }
    }

    @Nested
    class Supersede {

        @Test
        void shouldRevoke_whenOpenButExpired() {
            Invite invite = invite();

            invite.supersede();

            assertThat(invite.getStatus()).isEqualTo(InviteStatus.REVOKED);
        }

        @Test
        void shouldKeepUsed_whenAlreadyUsed() {
            Invite invite = invite();
            invite.use(NOW);

            invite.supersede();

            assertThat(invite.getStatus()).isEqualTo(InviteStatus.USED);
        }
    }
}
