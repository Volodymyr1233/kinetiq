package com.kinetiq.quiz.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.exception.QuizAlreadySubmittedException;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.model.QuizSubmissionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

// rows are read back with SQL, so the test sees what is stored rather than Hibernate's cached entity
@DataJpaTest
@Import({TestcontainersConfiguration.class, QuizSubmissionRepositoryImpl.class, QuizSubmissionMapperImpl.class})
class QuizSubmissionRepositoryImplIT {

    private static final Instant SUBMITTED_AT = Instant.parse("2026-10-30T10:00:00Z");

    @Autowired
    private QuizSubmissionRepositoryImpl repository;

    @Autowired
    private QuizSubmissionMapper mapper;

    @Autowired
    private JpaQuizSubmissionRepository jpa;

    @Autowired
    private JdbcTemplate jdbc;

    private AccountId insertClient() {
        UUID trainer = UUID.randomUUID();
        UUID client = UUID.randomUUID();
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, 'Marek Nowak', ?, 'hash', 'TRAINER', null, 'Europe/Warsaw', true, null, now(), now(), 0),
                       (?, 'Ola Kowalska', ?, 'hash', 'CLIENT', ?, 'Europe/Warsaw', true, now(), now(), now(), 0)
                """,
                trainer,
                trainer + "@example.com",
                client,
                client + "@example.com",
                trainer);
        return new AccountId(client);
    }

    private static QuizSubmission submission(AccountId client, String notes) {
        return new QuizSubmission(
                QuizSubmissionId.random(),
                client,
                Goal.BUILD_MUSCLE,
                Experience.YEARS_1_TO_2,
                Level.INTERMEDIATE,
                29,
                168,
                new BigDecimal("62.5"),
                Set.of(Like.WEIGHTS, Like.YOGA_PILATES),
                3,
                notes,
                SUBMITTED_AT);
    }

    private Map<String, Object> row(QuizSubmission submission) {
        return jdbc.queryForMap("select * from quiz_submissions where id = ?", submission.getId().value());
    }

    @Test
    void shouldStoreEveryAnswer_whenSaved() {
        QuizSubmission submission = submission(insertClient(), "Left knee");

        repository.save(submission);

        assertThat(row(submission))
                .containsEntry("client_id", submission.getClient().value())
                .containsEntry("goal", "BUILD_MUSCLE")
                .containsEntry("experience", "YEARS_1_TO_2")
                .containsEntry("level", "INTERMEDIATE")
                .containsEntry("age", 29)
                .containsEntry("height_cm", 168)
                .containsEntry("weight_kg", new BigDecimal("62.5"))
                .containsEntry("days_per_week", 3)
                .containsEntry("notes", "Left knee");
        assertThat(jdbc.queryForList(
                        "select like_code from quiz_submission_likes where submission_id = ?",
                        String.class,
                        submission.getId().value()))
                .containsExactlyInAnyOrder("WEIGHTS", "YOGA_PILATES");
    }

    @Test
    void shouldStoreNull_whenThereAreNoNotes() {
        QuizSubmission submission = submission(insertClient(), "   ");

        repository.save(submission);

        assertThat(row(submission)).containsEntry("notes", null);
    }

    @Test
    void shouldTellWhetherAClientHasSubmitted() {
        AccountId submitted = insertClient();
        AccountId notSubmitted = insertClient();
        repository.save(submission(submitted, null));

        assertThat(repository.existsByClient(submitted)).isTrue();
        assertThat(repository.existsByClient(notSubmitted)).isFalse();
    }

    // the service checks first; this is the submit that raced past that check
    @Test
    void shouldRefuseAsAlreadySubmitted_whenTheClientHasASubmission() {
        AccountId client = insertClient();
        repository.save(submission(client, null));

        assertThatThrownBy(() -> repository.save(submission(client, null)))
                .isInstanceOf(QuizAlreadySubmittedException.class);
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenAgeIsOutOfRange() {
        QuizSubmissionEntity entity = mapper.toEntity(submission(insertClient(), null));
        entity.setAge(15);

        assertThatThrownBy(() -> jpa.saveAndFlush(entity)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenALikeIsUnknown() {
        QuizSubmission submission = submission(insertClient(), null);
        repository.save(submission);

        assertThatThrownBy(() -> jdbc.update(
                        "insert into quiz_submission_likes (submission_id, like_code) values (?, 'SWIMMING')",
                        submission.getId().value()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
