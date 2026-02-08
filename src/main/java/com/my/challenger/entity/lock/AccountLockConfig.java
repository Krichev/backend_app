package com.my.challenger.entity.lock;

import com.my.challenger.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_lock_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLockConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "configured_by")
    private User configuredBy;

    @Column(name = "allow_self_unlock", nullable = false)
    @Builder.Default
    private Boolean allowSelfUnlock = true;

    @Column(name = "allow_emergency_bypass", nullable = false)
    @Builder.Default
    private Boolean allowEmergencyBypass = true;

    @Column(name = "max_emergency_bypasses_per_month", nullable = false)
    @Builder.Default
    private Integer maxEmergencyBypassesPerMonth = 3;

    @Column(name = "unlock_penalty_multiplier", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal unlockPenaltyMultiplier = new BigDecimal("2.00");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "require_approval_from")
    private User requireApprovalFrom;

    @Column(name = "escalation_enabled", nullable = false)
    @Builder.Default
    private Boolean escalationEnabled = false;

    @Column(name = "escalation_after_attempts", nullable = false)
    @Builder.Default
    private Integer escalationAfterAttempts = 3;

    @Column(name = "emergency_bypasses_used_this_month", nullable = false)
    @Builder.Default
    private Integer emergencyBypassesUsedThisMonth = 0;

    @Column(name = "bypass_month_reset_date", nullable = false)
    @Builder.Default
    private LocalDate bypassMonthResetDate = LocalDate.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
