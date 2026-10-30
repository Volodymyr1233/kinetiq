package com.kinetiq.quiz.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.exception.InvalidValueException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QuizSubmissionTest {

    private static final Set<Like> LIKES = Set.of(Like.WEIGHTS, Like.YOGA_PILATES);

    private static QuizSubmission submission(int age, int heightCm, String weightKg, Set<Like> likes, int days, String notes) {
        return new QuizSubmission(
                QuizSubmissionId.random(),
                AccountId.random(),
                Goal.BUILD_MUSCLE,
                Experience.YEARS_1_TO_2,
                Level.INTERMEDIATE,
                age,
                heightCm,
                new BigDecimal(weightKg),
                likes,
                days,
                notes,
                Instant.parse("2026-10-30T10:00:00Z"));
    }

    @Nested
    class Age {

        @ParameterizedTest
        @ValueSource(ints = {15, 100})
        void shouldRefuse_whenOutside16To99(int age) {
            assertThatThrownBy(() -> submission(age, 168, "62.5", LIKES, 3, null))
                    .isInstanceOf(InvalidValueException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {16, 99})
        void shouldAccept_whenAtABound(int age) {
            assertThat(submission(age, 168, "62.5", LIKES, 3, null).getAge()).isEqualTo(age);
        }
    }

    @Nested
    class Height {

        @ParameterizedTest
        @ValueSource(ints = {119, 231})
        void shouldRefuse_whenOutside120To230(int heightCm) {
            assertThatThrownBy(() -> submission(29, heightCm, "62.5", LIKES, 3, null))
                    .isInstanceOf(InvalidValueException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {120, 230})
        void shouldAccept_whenAtABound(int heightCm) {
            assertThat(submission(29, heightCm, "62.5", LIKES, 3, null).getHeightCm()).isEqualTo(heightCm);
        }
    }

    @Nested
    class Weight {

        @ParameterizedTest
        @ValueSource(strings = {"29.9", "250.1"})
        void shouldRefuse_whenOutside30To250(String weightKg) {
            assertThatThrownBy(() -> submission(29, 168, weightKg, LIKES, 3, null))
                    .isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldRefuse_whenItHasTwoDecimals() {
            assertThatThrownBy(() -> submission(29, 168, "62.55", LIKES, 3, null))
                    .isInstanceOf(InvalidValueException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"30", "30.0", "250.0", "62.50"})
        void shouldAcceptWithOneDecimal_whenInRange(String weightKg) {
            BigDecimal stored = submission(29, 168, weightKg, LIKES, 3, null).getWeightKg();

            assertThat(stored).isEqualByComparingTo(weightKg);
            assertThat(stored.scale()).isEqualTo(1);
        }
    }

    @Nested
    class Likes {

        @Test
        void shouldRefuse_whenEmpty() {
            assertThatThrownBy(() -> submission(29, 168, "62.5", Set.of(), 3, null))
                    .isInstanceOf(InvalidValueException.class);
        }

        @Test
        void shouldAcceptEveryCode() {
            assertThat(submission(29, 168, "62.5", Set.of(Like.values()), 3, null).getLikes())
                    .containsExactlyInAnyOrder(Like.values());
        }
    }

    @Nested
    class DaysPerWeek {

        @ParameterizedTest
        @ValueSource(ints = {1, 7})
        void shouldRefuse_whenOutside2To6(int days) {
            assertThatThrownBy(() -> submission(29, 168, "62.5", LIKES, days, null))
                    .isInstanceOf(InvalidValueException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {2, 6})
        void shouldAccept_whenAtABound(int days) {
            assertThat(submission(29, 168, "62.5", LIKES, days, null).getDaysPerWeek()).isEqualTo(days);
        }
    }

    @Nested
    class Notes {

        @Test
        void shouldBeTrimmed() {
            assertThat(submission(29, 168, "62.5", LIKES, 3, "  Left knee  ").getNotes()).contains("Left knee");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", " \n"})
        void shouldBeAbsent_whenEmptyAfterTrimming(String notes) {
            assertThat(submission(29, 168, "62.5", LIKES, 3, notes).getNotes()).isEmpty();
        }

        @Test
        void shouldBeAbsent_whenNull() {
            assertThat(submission(29, 168, "62.5", LIKES, 3, null).getNotes()).isEmpty();
        }

        @Test
        void shouldAccept500Units_whenTrimmed() {
            assertThat(submission(29, 168, "62.5", LIKES, 3, " " + "a".repeat(500) + " ").getNotes())
                    .hasValueSatisfying(notes -> assertThat(notes).hasSize(500));
        }

        @Test
        void shouldRefuse501Units() {
            assertThatThrownBy(() -> submission(29, 168, "62.5", LIKES, 3, "a".repeat(501)))
                    .isInstanceOf(InvalidValueException.class);
        }
    }
}
