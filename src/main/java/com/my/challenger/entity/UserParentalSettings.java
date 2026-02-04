package com.my.challenger.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_parental_settings",
    indexes = {
        @Index(name = "idx_parental_parent_user", columnList = "parent_user_id"),
        @Index(name = "idx_parental_is_child", columnList = "is_child_account")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user"})
@ToString(exclude = {"user"})
public class UserParentalSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_parental_settings_user"))
    private User user;

    @Column(name = "is_child_account", nullable = false)
    @Builder.Default
    private Boolean isChildAccount = false;

    @Column(name = "parent_user_id")
    private Long parentUserId;

    @Column(name = "age_group", length = 20)
    @Builder.Default
    private String ageGroup = "ADULT"; // UNDER_13, 13_TO_17, ADULT

    @Column(name = "content_restriction_level", length = 20)
    @Builder.Default
    private String contentRestrictionLevel = "NONE"; // STRICT, MODERATE, NONE

    @Column(name = "require_parent_approval", nullable = false)
    @Builder.Default
    private Boolean requireParentApproval = false;

    @Column(name = "allowed_topic_categories", columnDefinition = "TEXT")
    private String allowedTopicCategories; // JSON array or comma-separated

    @Column(name = "blocked_topic_categories", columnDefinition = "TEXT")
    private String blockedTopicCategories; // JSON array or comma-separated

    @Column(name = "max_daily_screen_time_minutes")
    private Integer maxDailyScreenTimeMinutes; // null = unlimited

    @Column(name = "max_daily_quiz_count")
    private Integer maxDailyQuizCount; // null = unlimited

    @Column(name = "parent_pin_hash", length = 255)
    private String parentPinHash;

    @Column(name = "last_parent_verification")
    private LocalDateTime lastParentVerification;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========== HELPER METHODS ==========

    public boolean isRestricted() {
        return Boolean.TRUE.equals(isChildAccount) && 
               !"NONE".equals(contentRestrictionLevel);
    }

    public boolean hasScreenTimeLimit() {
        return maxDailyScreenTimeMinutes != null && maxDailyScreenTimeMinutes > 0;
    }

    public boolean hasQuizLimit() {
        return maxDailyQuizCount != null && maxDailyQuizCount > 0;
    }

    public boolean needsParentApproval() {
        return Boolean.TRUE.equals(isChildAccount) && 
               Boolean.TRUE.equals(requireParentApproval);
    }
}
