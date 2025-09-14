// src/main/java/com/my/challenger/entity/quiz/QuizQuestion.java
package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuizDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challenge_id")
    private Long challengeId;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(nullable = false, length = 500)
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizDifficulty difficulty;

    @Column(length = 200)
    private String topic;

    @Column(length = 500)
    private String source; // Source identifier (e.g., "APP_GENERATED", "USER_CREATED")

    @Column(name = "additional_info", length = 1000)
    private String additionalInfo;

    @Column(name = "is_user_created", nullable = false)
    private Boolean isUserCreated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(name = "external_id", length = 100)
    private String externalId; // For tracking original question ID

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0; // Track how many times this question has been used

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_used")
    private LocalDateTime lastUsed; // Track when this question was last used

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (usageCount == null) {
            usageCount = 0;
        }
        if (isUserCreated == null) {
            isUserCreated = false;
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Increment usage count and update last used timestamp
     */
    public void incrementUsage() {
        this.usageCount = (this.usageCount != null ? this.usageCount : 0) + 1;
        this.lastUsed = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}