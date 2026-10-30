package com.kinetiq.quiz.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.exception.InvalidValueException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;

/** One client's answers to the quiz. It never changes once submitted (spec 54, FR-009). */
@Getter
public class QuizSubmission {

    public static final int MIN_AGE = 16;
    public static final int MAX_AGE = 99;
    public static final int MIN_HEIGHT_CM = 120;
    public static final int MAX_HEIGHT_CM = 230;
    public static final BigDecimal MIN_WEIGHT_KG = new BigDecimal("30.0");
    public static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("250.0");
    public static final int MIN_DAYS_PER_WEEK = 2;
    public static final int MAX_DAYS_PER_WEEK = 6;
    public static final int NOTES_MAX_LENGTH = 500;

    // also trims no-break and other Unicode spaces, which strip() keeps, as the review's trim() does
    private static final Pattern EDGE_SPACES = Pattern.compile("^[\\s\\p{Z}]+|[\\s\\p{Z}]+$");

    private final QuizSubmissionId id;
    private final AccountId client;
    private final Goal goal;
    private final Experience experience;
    private final Level level;
    private final int age;
    private final int heightCm;
    private final BigDecimal weightKg;
    private final Set<Like> likes;
    private final int daysPerWeek;

    @Getter(AccessLevel.NONE)
    private final String notes;

    private final Instant submittedAt;

    /**
     * Creates a submission, checking every rule of spec 50 (data model, "Submitted answers"). {@code notes} may be
     * {@code null}; they are trimmed, and empty notes are none.
     */
    public QuizSubmission(
            QuizSubmissionId id,
            AccountId client,
            Goal goal,
            Experience experience,
            Level level,
            int age,
            int heightCm,
            BigDecimal weightKg,
            Set<Like> likes,
            int daysPerWeek,
            String notes,
            Instant submittedAt) {
        if (outside(age, MIN_AGE, MAX_AGE)) {
            throw new InvalidValueException("age out of range");
        }
        if (outside(heightCm, MIN_HEIGHT_CM, MAX_HEIGHT_CM)) {
            throw new InvalidValueException("height out of range");
        }
        if (weightKg.compareTo(MIN_WEIGHT_KG) < 0
                || weightKg.compareTo(MAX_WEIGHT_KG) > 0
                || weightKg.stripTrailingZeros().scale() > 1) {
            throw new InvalidValueException("weight out of range or with more than one decimal");
        }
        if (likes.isEmpty()) {
            throw new InvalidValueException("no likes");
        }
        if (outside(daysPerWeek, MIN_DAYS_PER_WEEK, MAX_DAYS_PER_WEEK)) {
            throw new InvalidValueException("days per week out of range");
        }
        String trimmed = notes == null ? "" : EDGE_SPACES.matcher(notes).replaceAll("");
        if (trimmed.length() > NOTES_MAX_LENGTH) {
            throw new InvalidValueException("notes are too long");
        }
        this.id = Objects.requireNonNull(id);
        this.client = Objects.requireNonNull(client);
        this.goal = Objects.requireNonNull(goal);
        this.experience = Objects.requireNonNull(experience);
        this.level = Objects.requireNonNull(level);
        this.age = age;
        this.heightCm = heightCm;
        this.weightKg = weightKg.setScale(1, RoundingMode.UNNECESSARY);
        this.likes = Set.copyOf(likes);
        this.daysPerWeek = daysPerWeek;
        this.notes = trimmed.isEmpty() ? null : trimmed;
        this.submittedAt = Objects.requireNonNull(submittedAt);
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }

    private static boolean outside(int value, int min, int max) {
        return value < min || value > max;
    }
}
