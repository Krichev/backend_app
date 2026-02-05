package com.my.challenger.entity.vibration;

import com.my.challenger.entity.enums.VibrationDifficulty;
import com.my.challenger.entity.enums.VibrationSessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vibration_game_sessions", indexes = {
        @Index(name = "idx_vibration_sessions_user", columnList = "user_id"),
        @Index(name = "idx_vibration_sessions_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VibrationGameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private VibrationDifficulty difficulty;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    @Column(name = "max_replays_per_question")
    @Builder.Default
    private Integer maxReplaysPerQuestion = 3;

    @Column(name = "guess_time_limit_seconds")
    @Builder.Default
    private Integer guessTimeLimitSeconds = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private VibrationSessionStatus status = VibrationSessionStatus.ACTIVE;

    @Column(name = "current_question_index")
    @Builder.Default
    private Integer currentQuestionIndex = 0;

    @Column(name = "total_score")
    @Builder.Default
    private Integer totalScore = 0;

    @Column(name = "correct_answers")
    @Builder.Default
    private Integer correctAnswers = 0;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    @Builder.Default
    private List<VibrationSessionQuestion> questions = new ArrayList<>();
}
