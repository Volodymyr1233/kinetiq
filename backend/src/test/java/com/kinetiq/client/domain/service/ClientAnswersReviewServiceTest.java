package com.kinetiq.client.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.client.domain.output.ClientDirectory;
import com.kinetiq.quiz.domain.exception.QuizNotFoundException;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.model.QuizSubmissionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClientAnswersReviewServiceTest {

    private static final AccountId MAREK = AccountId.random();
    private static final AccountId OLA_TRAINER = AccountId.random();
    private static final AccountId ANNA = AccountId.random();
    private static final AccountId BASIA = AccountId.random();

    private final Map<AccountId, QuizSubmission> answers = new HashMap<>();

    // Anna and Basia are both Marek's clients; only Anna has answered the quiz
    private final ClientDirectory directory = new ClientDirectory() {
        @Override
        public List<ClientSummary> findClientsOf(AccountId trainer) {
            return List.of();
        }

        @Override
        public Optional<ClientSummary> findClient(AccountId trainer, AccountId client) {
            boolean own = MAREK.equals(trainer) && (ANNA.equals(client) || BASIA.equals(client));
            return own
                    ? Optional.of(new ClientSummary(client, "Client", null, PlanState.NO_QUIZ, ZoneId.of("UTC")))
                    : Optional.empty();
        }
    };

    private final ClientAnswersReviewService service =
            new ClientAnswersReviewService(directory, client -> Optional.ofNullable(answers.get(client)));

    private static QuizSubmission answersOf(AccountId client) {
        return new QuizSubmission(
                QuizSubmissionId.random(),
                client,
                Goal.LOSE_FAT,
                Experience.YEARS_1_TO_2,
                Level.INTERMEDIATE,
                30,
                178,
                new BigDecimal("80.0"),
                Set.of(Like.WEIGHTS),
                3,
                "Left knee",
                Instant.parse("2026-11-05T10:00:00Z"));
    }

    @Test
    void shouldReturnTheAnswers_whenTheClientIsTheTrainersOwn() {
        QuizSubmission annas = answersOf(ANNA);
        answers.put(ANNA, annas);

        assertThat(service.answers(MAREK, ANNA)).isSameAs(annas);
    }

    // the answers exist, so only the ownership check keeps them from another trainer
    @Test
    void shouldRefuseAsClientNotFound_whenTheClientIsAnotherTrainersOrUnknown() {
        answers.put(ANNA, answersOf(ANNA));

        assertThatThrownBy(() -> service.answers(OLA_TRAINER, ANNA)).isInstanceOf(ClientNotFoundException.class);
        assertThatThrownBy(() -> service.answers(MAREK, AccountId.random()))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void shouldRefuseAsQuizNotFound_whenAnOwnClientHasNoAnswers() {
        assertThatThrownBy(() -> service.answers(MAREK, BASIA)).isInstanceOf(QuizNotFoundException.class);
    }
}
