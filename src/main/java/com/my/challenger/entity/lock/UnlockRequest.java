package com.my.challenger.entity.lock;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.UnlockRequestStatus;
import com.my.challenger.entity.enums.UnlockType;
import com.my.challenger.entity.penalty.Penalty;
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
import java.util.Map;

@Entity
@Table(name = "unlock_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnlockRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_id")
    private Penalty penalty;

    @Enumerated(EnumType.STRING)
    @Column(name = "unlock_type", nullable = false)
    private UnlockType unlockType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UnlockRequestStatus status = UnlockRequestStatus.PENDING;

    @Column(name = "payment_type", length = 30)
    private String paymentType;

    @Column(name = "payment_amount")
    private Integer paymentAmount;

    @Column(name = "payment_fulfilled")
    @Builder.Default
    private Boolean paymentFulfilled = false;

    @Column(name = "bypass_number")
    private Integer bypassNumber;

    @Column(name = "reason")
    private String reason;

    @Column(name = "approver_message")
    private String approverMessage;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_info", columnDefinition = "jsonb")
    private Map<String, Object> deviceInfo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
