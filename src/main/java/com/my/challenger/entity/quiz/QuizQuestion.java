// src/main/java/com/my/challenger/entity/quiz/QuizQuestion.java
package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuizDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quiz_questions")
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
    private QuizDifficulty difficulty;

    @Column(length = 200)
    private String topic;

    @Column(length = 500)
    private String source;

    @Column(length = 1000)
    private String additionalInfo;

    @Column(name = "is_user_created")
    private Boolean isUserCreated = false;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(name = "external_id")
    private String externalId; // For questions from external APIs

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}