package com.my.challenger.entity.parental;

import com.my.challenger.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "child_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_user_id", nullable = false, unique = true)
    private User child;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_by_parent_id", nullable = false)
    private User managedByParent;

    @Column(name = "daily_budget_minutes", nullable = false)
    private Integer dailyBudgetMinutes = 180;

    @Column(name = "max_wager_amount")
    private BigDecimal maxWagerAmount = new BigDecimal("100.00");

    @Column(name = "allow_money_wagers")
    private boolean allowMoneyWagers = false;

    @Column(name = "allow_screen_time_wagers")
    private boolean allowScreenTimeWagers = true;
    
    @Column(name = "allow_social_wagers")
    private boolean allowSocialWagers = true;

    @Column(name = "max_extension_requests_per_day")
    private Integer maxExtensionRequestsPerDay = 3;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "restricted_categories", columnDefinition = "jsonb")
    private List<String> restrictedCategories;

    @Column(name = "content_age_rating", length = 10)
    private String contentAgeRating = "E";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notifications_to_parent", columnDefinition = "jsonb")
    private Map<String, Boolean> notificationsToParent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (restrictedCategories == null) restrictedCategories = List.of();
        if (notificationsToParent == null) {
            notificationsToParent = Map.of(
                "onLowTime", true,
                "onWager", true,
                "dailySummary", true,
                "onPenalty", true
            );
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
