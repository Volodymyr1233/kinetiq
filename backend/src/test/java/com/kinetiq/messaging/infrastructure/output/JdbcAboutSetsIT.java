package com.kinetiq.messaging.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.messaging.domain.model.AboutSet;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JdbcAboutSets.class})
class JdbcAboutSetsIT {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 2);

    @Autowired
    private JdbcAboutSets aboutSets;

    @Autowired
    private JdbcTemplate jdbc;

    private AccountId trainer;
    private AccountId client;
    private MessagingFixture.LoggedSet logged;

    @BeforeEach
    void setUp() {
        MessagingFixture fixture = new MessagingFixture(jdbc);
        trainer = fixture.trainer("Marek");
        client = fixture.client("Anna", trainer);
        logged = fixture.setLog(client, DAY, 9);
    }

    @Test
    void shouldAnswerTheSetWithItsSessionExerciseAndPlace_whenTheSenderLoggedIt() {
        assertThat(aboutSets.ofSender(client, new SetLogId(logged.id())))
                .contains(new AboutSet(
                        new SetLogId(logged.id()),
                        new SessionLogId(logged.sessionLog()),
                        "Lower body",
                        DAY,
                        new LocalizedText("Back squat", "Przysiad"),
                        0,
                        3,
                        9));
    }

    @Test
    void shouldAnswerNothing_whenTheSetIsAnotherClients() {
        AccountId other = new MessagingFixture(jdbc).client("Basia", trainer);

        assertThat(aboutSets.ofSender(other, new SetLogId(logged.id()))).isEmpty();
    }

    // the trainer may read the client's logs, but did not log the set
    @Test
    void shouldAnswerNothing_whenTheSenderIsTheClientsTrainer() {
        assertThat(aboutSets.ofSender(trainer, new SetLogId(logged.id()))).isEmpty();
    }

    @Test
    void shouldAnswerNothing_whenTheSetIsUnknown() {
        assertThat(aboutSets.ofSender(client, new SetLogId(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void shouldAnswerTheSetsOfTheGivenIdsOnly() {
        MessagingFixture.LoggedSet later = new MessagingFixture(jdbc).setLog(client, DAY.plusDays(1), 7);
        new MessagingFixture(jdbc).setLog(client, DAY.plusDays(2), 8);

        assertThat(aboutSets.byIds(List.of(logged.id(), later.id())).keySet())
                .containsExactlyInAnyOrder(logged.id(), later.id());
        assertThat(aboutSets.byIds(List.of())).isEmpty();
    }
}
