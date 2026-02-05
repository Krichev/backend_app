package com.my.challenger.entity.competitive;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.CompetitiveRoundStatus;
import com.my.challenger.entity.quiz.QuizQuestion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "competitive_match_rounds", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "round_number"}))
public class CompetitiveMatchRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private CompetitiveMatch match;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "competitive_round_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private CompetitiveRoundStatus status = CompetitiveRoundStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuizQuestion question;

    // Player 1 Scores
    @Column(name = "player1_score")
    private BigDecimal player1Score;

    @Column(name = "player1_pitch_score")
    private BigDecimal player1PitchScore;

    @Column(name = "player1_rhythm_score")
    private BigDecimal player1RhythmScore;

    @Column(name = "player1_voice_score")
    private BigDecimal player1VoiceScore;

    @Column(name = "player1_submission_path", length = 500)
    private String player1SubmissionPath;

    @Column(name = "player1_submitted_at")
    private LocalDateTime player1SubmittedAt;

    // Player 2 Scores
    @Column(name = "player2_score")
    private BigDecimal player2Score;

    @Column(name = "player2_pitch_score")
    private BigDecimal player2PitchScore;

    @Column(name = "player2_rhythm_score")
    private BigDecimal player2RhythmScore;

    @Column(name = "player2_voice_score")
    private BigDecimal player2VoiceScore;

    @Column(name = "player2_submission_path", length = 500)
    private String player2SubmissionPath;

    @Column(name = "player2_submitted_at")
    private LocalDateTime player2SubmittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_winner_id")
    private User roundWinner;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
