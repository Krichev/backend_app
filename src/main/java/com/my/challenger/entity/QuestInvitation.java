package com.my.challenger.entity;

import com.my.challenger.entity.enums.QuestInvitationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "quest_invitations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @Column(name = "proposed_stake_type")
    private String proposedStakeType;

    @Column(name = "proposed_stake_amount")
    private BigDecimal proposedStakeAmount;

    @Column(name = "proposed_stake_currency")
    private String proposedStakeCurrency;

    @Column(name = "proposed_screen_time_minutes")
    private Integer proposedScreenTimeMinutes;

    @Column(name = "proposed_social_penalty_description")
    private String proposedSocialPenaltyDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "quest_invitation_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private QuestInvitationStatus status = QuestInvitationStatus.PENDING;

    private String message;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InvitationNegotiation> negotiations = new HashSet<>();
}
