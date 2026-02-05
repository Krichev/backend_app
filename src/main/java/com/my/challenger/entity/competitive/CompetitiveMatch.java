package com.my.challenger.entity.competitive;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.AudioChallengeType;
import com.my.challenger.entity.enums.CompetitiveMatchStatus;
import com.my.challenger.entity.enums.CompetitiveMatchType;
import com.my.challenger.entity.wager.Wager;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "competitive_matches")
public class CompetitiveMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, columnDefinition = "competitive_match_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CompetitiveMatchType matchType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "competitive_match_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private CompetitiveMatchStatus status = CompetitiveMatchStatus.WAITING_FOR_OPPONENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id", nullable = false)
    private User player1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id")
    private User player2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(name = "total_rounds", nullable = false)
    @Builder.Default
    private Integer totalRounds = 1;

    @Column(name = "current_round", nullable = false)
    @Builder.Default
    private Integer currentRound = 0;

    @Column(name = "player1_total_score")
    @Builder.Default
    private BigDecimal player1TotalScore = BigDecimal.ZERO;

    @Column(name = "player2_total_score")
    @Builder.Default
    private BigDecimal player2TotalScore = BigDecimal.ZERO;

    @Column(name = "player1_rounds_won")
    @Builder.Default
    private Integer player1RoundsWon = 0;

    @Column(name = "player2_rounds_won")
    @Builder.Default
    private Integer player2RoundsWon = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wager_id")
    private Wager wager;

    @Enumerated(EnumType.STRING)
    @Column(name = "audio_challenge_type")
    @Builder.Default
    private AudioChallengeType audioChallengeType = AudioChallengeType.SINGING;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CompetitiveMatchRound> rounds = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = CompetitiveMatchStatus.WAITING_FOR_OPPONENT;
        }
        if (totalRounds == null) {
            totalRounds = 1;
        }
        if (currentRound == null) {
            currentRound = 0;
        }
    }
}
