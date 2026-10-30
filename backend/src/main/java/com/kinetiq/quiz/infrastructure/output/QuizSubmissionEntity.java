package com.kinetiq.quiz.infrastructure.output;

import com.kinetiq.common.infrastructure.output.BaseEntity;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quiz_submissions")
class QuizSubmissionEntity extends BaseEntity {

    @Id
    private UUID id;

    private UUID clientId;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    private Experience experience;

    @Enumerated(EnumType.STRING)
    private Level level;

    private int age;
    private int heightCm;
    private BigDecimal weightKg;

    @ElementCollection
    @CollectionTable(name = "quiz_submission_likes", joinColumns = @JoinColumn(name = "submission_id"))
    @Column(name = "like_code")
    @Enumerated(EnumType.STRING)
    private Set<Like> likes = new HashSet<>();

    private int daysPerWeek;
    private String notes;
    private Instant submittedAt;
}
