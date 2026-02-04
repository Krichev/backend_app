package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.BrainRingRoundStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "brain_ring_round_state")
public class BrainRingRoundState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "quiz_round_id", nullable = false, unique = true)
    private QuizRound quizRound;

    @ManyToOne
    @JoinColumn(name = "current_buzzer_user_id")
    private User currentBuzzer;

    @Column(name = "buzzer_timestamp")
    private Instant buzzerTimestamp;

    @Column(name = "answer_deadline")
    private Instant answerDeadline;

    @Column(name = "locked_out_players", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private String lockedOutPlayers = "[]";

    @Column(name = "buzz_order", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private String buzzOrder = "[]";

    @Enumerated(EnumType.STRING)
    @Column(name = "round_status", columnDefinition = "brain_ring_round_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private BrainRingRoundStatus roundStatus = BrainRingRoundStatus.WAITING_FOR_BUZZ;

    @ManyToOne
    @JoinColumn(name = "winner_user_id")
    private User winner;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (roundStatus == null) {
            roundStatus = BrainRingRoundStatus.WAITING_FOR_BUZZ;
        }
        if (lockedOutPlayers == null) {
            lockedOutPlayers = "[]";
        }
        if (buzzOrder == null) {
            buzzOrder = "[]";
        }
    }
}
