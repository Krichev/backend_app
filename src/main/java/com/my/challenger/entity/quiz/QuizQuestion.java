// src/main/java/com/my/challenger/entity/quiz/QuizQuestion.java
package com.my.challenger.entity.quiz;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class QuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(nullable = false, length = 500)
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizDifficulty difficulty;

    @Column(length = 100)
    private String topic;

    @Column(length = 100)
    private String source;

    @Column(length = 500)
    private String additionalInfo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isUserCreated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "external_id", length = 100)
    private String externalId;

    // New multimedia fields
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    @Builder.Default
    private QuestionType questionType = QuestionType.TEXT;

    @Column(name = "question_media_url", length = 500)
    private String questionMediaUrl;

    @Column(name = "question_media_id", length = 100)
    private String questionMediaId;

    @Column(name = "question_media_type", length = 50)
    private String questionMediaType;

    @Column(name = "question_thumbnail_url", length = 500)
    private String questionThumbnailUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods for multimedia
    public boolean hasMedia() {
        return questionMediaUrl != null && !questionMediaUrl.trim().isEmpty();
    }

    public boolean isVideoQuestion() {
        return questionType == QuestionType.VIDEO;
    }

    public boolean isAudioQuestion() {
        return questionType == QuestionType.AUDIO;
    }

    public boolean isImageQuestion() {
        return questionType == QuestionType.IMAGE;
    }

    public boolean isTextOnlyQuestion() {
        return questionType == QuestionType.TEXT;
    }
}