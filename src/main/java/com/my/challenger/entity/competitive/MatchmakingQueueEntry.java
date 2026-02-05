package com.my.challenger.entity.competitive;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.AudioChallengeType;
import com.my.challenger.entity.enums.MatchmakingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "matchmaking_queue")
public class MatchmakingQueueEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "matchmaking_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private MatchmakingStatus status = MatchmakingStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(name = "audio_challenge_type", nullable = false)
    private AudioChallengeType audioChallengeType;

    @Column(name = "preferred_rounds", nullable = false)
    @Builder.Default
    private Integer preferredRounds = 1;

    @Column(name = "skill_rating")
    private Integer skillRating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_with_user_id")
    private User matchedWithUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_match_id")
    private CompetitiveMatch matchedMatch;

    @Column(name = "queued_at")
    @Builder.Default
    private LocalDateTime queuedAt = LocalDateTime.now();

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
