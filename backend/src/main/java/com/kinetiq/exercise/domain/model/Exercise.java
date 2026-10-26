package com.kinetiq.exercise.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.exception.InvalidValueException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Exercise {

    public static final int NAME_MAX_LENGTH = 100;
    public static final int EQUIPMENT_MAX_LENGTH = 50;
    public static final int CUES_MAX_LENGTH = 500;
    public static final int MAX_SETS = 20;
    public static final int MAX_AMOUNT = 3600;
    public static final int MAX_REST_SECONDS = 600;
    public static final int MAX_OWN_PER_TRAINER = 200;

    private final ExerciseId id;

    @Getter(AccessLevel.NONE)
    private final AccountId owner;

    private final LocalizedText name;
    private final Category category;
    private final LocalizedText equipment;
    private final Measure measure;
    private final int defaultSets;
    private final int defaultAmount;
    private final int restSeconds;
    private final LocalizedText cues;

    /** Creates an exercise; {@code owner} is {@code null} for a starter exercise. */
    public Exercise(
            ExerciseId id,
            AccountId owner,
            LocalizedText name,
            Category category,
            LocalizedText equipment,
            Measure measure,
            int defaultSets,
            int defaultAmount,
            int restSeconds,
            LocalizedText cues) {
        if (name.en().isBlank() || longerThan(name, NAME_MAX_LENGTH)) {
            throw new InvalidValueException("name is blank or too long");
        }
        if (longerThan(equipment, EQUIPMENT_MAX_LENGTH)) {
            throw new InvalidValueException("equipment is too long");
        }
        if (longerThan(cues, CUES_MAX_LENGTH)) {
            throw new InvalidValueException("cues are too long");
        }
        if (outside(defaultSets, 1, MAX_SETS)) {
            throw new InvalidValueException("default sets out of range");
        }
        if (outside(defaultAmount, 1, MAX_AMOUNT)) {
            throw new InvalidValueException("default amount out of range");
        }
        if (outside(restSeconds, 0, MAX_REST_SECONDS)) {
            throw new InvalidValueException("rest out of range");
        }
        this.id = Objects.requireNonNull(id);
        this.owner = owner;
        this.name = name;
        this.category = Objects.requireNonNull(category);
        this.equipment = equipment;
        this.measure = Objects.requireNonNull(measure);
        this.defaultSets = defaultSets;
        this.defaultAmount = defaultAmount;
        this.restSeconds = restSeconds;
        this.cues = cues;
    }

    public Optional<AccountId> getOwner() {
        return Optional.ofNullable(owner);
    }

    /** Tells whether this is a starter exercise, one with no owner that nobody can change. */
    public boolean isStarter() {
        return owner == null;
    }

    /** Tells whether the account owns this exercise; nobody owns a starter exercise. */
    public boolean isOwnedBy(AccountId account) {
        return owner != null && owner.equals(account);
    }

    /**
     * Returns this exercise with new values and the same id, owner and measure. The measure never changes, because it
     * gives the amounts in plans and logs their meaning.
     */
    public Exercise edit(
            LocalizedText name,
            Category category,
            LocalizedText equipment,
            int defaultSets,
            int defaultAmount,
            int restSeconds,
            LocalizedText cues) {
        return new Exercise(
                id, owner, name, category, equipment, measure, defaultSets, defaultAmount, restSeconds, cues);
    }

    /** Tells whether the text, trimmed and ignoring case, is part of the English or the Polish name; blank text matches. */
    public boolean matches(String text) {
        String needle = text.strip().toLowerCase(Locale.ROOT);
        return name.en().toLowerCase(Locale.ROOT).contains(needle)
                || name.pl().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean longerThan(LocalizedText text, int maxLength) {
        return text.en().length() > maxLength || text.pl().length() > maxLength;
    }

    private static boolean outside(int value, int min, int max) {
        return value < min || value > max;
    }
}
