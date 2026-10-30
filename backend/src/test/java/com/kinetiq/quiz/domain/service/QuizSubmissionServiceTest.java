package com.kinetiq.quiz.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.exception.QuizAlreadySubmittedException;
import com.kinetiq.quiz.domain.input.SubmitQuizCommand;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import com.kinetiq.quiz.domain.model.QuizStatus;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QuizSubmissionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
    private static final AccountId CLIENT = AccountId.random();

    private final InMemoryQuizSubmissionRepository submissions = new InMemoryQuizSubmissionRepository();
    private final QuizSubmissionService service =
            new QuizSubmissionService(submissions, Clock.fixed(NOW, ZoneOffset.UTC));

    private static SubmitQuizCommand command(Goal goal) {
        return new SubmitQuizCommand(
                CLIENT,
                goal,
                Experience.YEARS_1_TO_2,
                Level.INTERMEDIATE,
                29,
                168,
                new BigDecimal("62.5"),
                Set.of(Like.WEIGHTS),
                3,
                "Left knee");
    }

    @Nested
    class Submit {

        @Test
        void shouldStoreTheAnswersWithTheTime_whenFirstSubmit() {
            QuizStatus status = service.submit(command(Goal.BUILD_MUSCLE));

            assertThat(status).isEqualTo(QuizStatus.WAITING_FOR_PLAN);
            QuizSubmission stored = submissions.byClient.get(CLIENT);
            assertThat(stored.getGoal()).isEqualTo(Goal.BUILD_MUSCLE);
            assertThat(stored.getNotes()).contains("Left knee");
            assertThat(stored.getSubmittedAt()).isEqualTo(NOW);
        }

        @Test
        void shouldRefuseAndKeepTheStoredAnswers_whenClientSubmittedBefore() {
            service.submit(command(Goal.BUILD_MUSCLE));

            assertThatThrownBy(() -> service.submit(command(Goal.LOSE_FAT)))
                    .isInstanceOf(QuizAlreadySubmittedException.class);
            assertThat(submissions.byClient.get(CLIENT).getGoal()).isEqualTo(Goal.BUILD_MUSCLE);
        }
    }

    @Nested
    class Status {

        @Test
        void shouldBeNotSubmitted_whenNoSubmission() {
            assertThat(service.status(CLIENT)).isEqualTo(QuizStatus.NOT_SUBMITTED);
        }

        @Test
        void shouldBeWaitingForPlan_whenSubmitted() {
            service.submit(command(Goal.BUILD_MUSCLE));

            assertThat(service.status(CLIENT)).isEqualTo(QuizStatus.WAITING_FOR_PLAN);
        }
    }
}
