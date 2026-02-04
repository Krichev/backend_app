package com.my.challenger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "screen_time_budgets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenTimeBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "daily_budget_minutes", nullable = false)
    @Builder.Default
    private Integer dailyBudgetMinutes = 180;

    @Column(name = "available_minutes", nullable = false)
    @Builder.Default
    private Integer availableMinutes = 180;

    @Column(name = "locked_minutes", nullable = false)
    @Builder.Default
    private Integer lockedMinutes = 0;

    @Column(name = "lost_today_minutes", nullable = false)
    @Builder.Default
    private Integer lostTodayMinutes = 0;

    @Column(name = "won_today_minutes", nullable = false)
    @Builder.Default
    private Integer wonTodayMinutes = 0;

    @Column(name = "total_lost_minutes", nullable = false)
    @Builder.Default
    private Long totalLostMinutes = 0L;

    @Column(name = "total_won_minutes", nullable = false)
    @Builder.Default
    private Long totalWonMinutes = 0L;

    @Column(name = "last_reset_date", nullable = false)
    private LocalDate lastResetDate;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "timezone", nullable = false)
    @Builder.Default
    private String timezone = "UTC";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    /**
     * Checks if the user has available time remaining
     */
    public boolean hasAvailableTime() {
        return availableMinutes > 0;
    }

    /**
     * Gets effective available time (considering locks)
     * Note: lockedMinutes are already removed from availableMinutes when locking,
     * so availableMinutes IS the effective available time.
     */
    public Integer getEffectiveAvailable() {
        return availableMinutes;
    }

    /**
     * Checks if budget is fully locked (available is 0 but locked > 0)
     */
    public boolean isFullyLocked() {
        return availableMinutes == 0 && lockedMinutes > 0;
    }
}
