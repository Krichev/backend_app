package com.my.challenger.entity.wager;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.ParticipantWagerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wager_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"wager_id", "user_id"})
})
public class WagerParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wager_id", nullable = false)
    private Wager wager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private ParticipantWagerStatus status = ParticipantWagerStatus.INVITED;

    @Column(name = "stake_escrowed", nullable = false)
    @Builder.Default
    private boolean stakeEscrowed = false;

    @Column(name = "amount_won", precision = 10, scale = 2)
    private BigDecimal amountWon;

    @Column(name = "amount_lost", precision = 10, scale = 2)
    private BigDecimal amountLost;

    @Column(name = "quiz_score")
    private Integer quizScore;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;
}
