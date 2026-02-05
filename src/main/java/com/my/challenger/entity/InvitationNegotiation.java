package com.my.challenger.entity;

import com.my.challenger.entity.enums.NegotiationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invitation_negotiations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationNegotiation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", nullable = false)
    private QuestInvitation invitation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_id", nullable = false)
    private User proposer;

    @Column(name = "counter_stake_type")
    private String counterStakeType;

    @Column(name = "counter_stake_amount")
    private BigDecimal counterStakeAmount;

    @Column(name = "counter_stake_currency")
    private String counterStakeCurrency;

    @Column(name = "counter_screen_time_minutes")
    private Integer counterScreenTimeMinutes;

    @Column(name = "counter_social_penalty_description")
    private String counterSocialPenaltyDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "negotiation_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private NegotiationStatus status = NegotiationStatus.PROPOSED;

    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
