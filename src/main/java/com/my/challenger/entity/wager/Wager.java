package com.my.challenger.entity.wager;

import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.CurrencyType;
import com.my.challenger.entity.enums.StakeType;
import com.my.challenger.entity.enums.WagerStatus;
import com.my.challenger.entity.enums.WagerType;
import com.my.challenger.entity.quiz.QuizSession;
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
@Table(name = "wagers")
public class Wager {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_session_id")
    private QuizSession quizSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "wager_type", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private WagerType wagerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "stake_type", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StakeType stakeType;

    @Column(name = "stake_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal stakeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "stake_currency")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CurrencyType stakeCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private WagerStatus status = WagerStatus.PROPOSED;

    @Column(name = "min_participants", nullable = false)
    @Builder.Default
    private Integer minParticipants = 2;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "social_penalty_description")
    private String socialPenaltyDescription;

    @Column(name = "screen_time_minutes")
    private Integer screenTimeMinutes;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "wager", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WagerParticipant> participants = new ArrayList<>();
}
