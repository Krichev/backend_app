package com.my.challenger.entity.vibration;

import com.my.challenger.entity.enums.LeaderboardPeriod;
import com.my.challenger.entity.enums.VibrationDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vibration_leaderboard", indexes = {
        @Index(name = "idx_leaderboard_period", columnList = "period, period_start, difficulty"),
        @Index(name = "idx_leaderboard_score", columnList = "total_score DESC")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "period", "period_start", "difficulty"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VibrationLeaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false)
    private LeaderboardPeriod period;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private VibrationDifficulty difficulty;

    @Column(name = "total_score")
    @Builder.Default
    private Integer totalScore = 0;

    @Column(name = "games_played")
    @Builder.Default
    private Integer gamesPlayed = 0;

    @Column(name = "correct_answers")
    @Builder.Default
    private Integer correctAnswers = 0;

    @Column(name = "total_questions")
    @Builder.Default
    private Integer totalQuestions = 0;

    @Column(name = "best_streak")
    @Builder.Default
    private Integer bestStreak = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
