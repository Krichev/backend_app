// src/main/java/com/my/challenger/entity/quiz/QuizQuestion.java
package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.enums.MediaType;
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

    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    @Column(name = "is_user_created", nullable = false)
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

    // New multimedia fields with proper enum types
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    @Builder.Default
    private QuestionType questionType = QuestionType.TEXT;

    @Column(name = "question_media_url", length = 500)
    private String questionMediaUrl;

    @Column(name = "question_media_id", length = 100)
    private String questionMediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_media_type")
    private MediaType questionMediaType;

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

    // New enhanced helper methods
    public boolean hasVideoMedia() {
        return questionMediaType == MediaType.VIDEO;
    }

    public boolean hasImageMedia() {
        return questionMediaType == MediaType.IMAGE;
    }

    public boolean hasAudioMedia() {
        return questionMediaType == MediaType.AUDIO;
    }

    public boolean hasDocumentMedia() {
        return questionMediaType == MediaType.DOCUMENT;
    }

    /**
     * Check if question type and media type are consistent
     */
    public boolean isMediaTypeConsistent() {
        if (questionType == QuestionType.TEXT) {
            return questionMediaType == null;
        }

        if (questionType == QuestionType.IMAGE) {
            return questionMediaType == MediaType.IMAGE || questionMediaType == MediaType.QUIZ_QUESTION;
        }

        if (questionType == QuestionType.AUDIO) {
            return questionMediaType == MediaType.AUDIO || questionMediaType == MediaType.QUIZ_QUESTION;
        }

        if (questionType == QuestionType.VIDEO) {
            return questionMediaType == MediaType.VIDEO || questionMediaType == MediaType.QUIZ_QUESTION;
        }

        if (questionType == QuestionType.MULTIMEDIA) {
            return questionMediaType != null;
        }

        return true;
    }

    /**
     * Get the expected media type based on question type
     */
    public MediaType getExpectedMediaType() {
        switch (questionType) {
            case IMAGE:
                return MediaType.IMAGE;
            case AUDIO:
                return MediaType.AUDIO;
            case VIDEO:
                return MediaType.VIDEO;
            case MULTIMEDIA:
                return MediaType.QUIZ_QUESTION;
            case TEXT:
            default:
                return null;
        }
    }

    /**
     * Set media type automatically based on question type
     */
    public void autoSetMediaType() {
        this.questionMediaType = getExpectedMediaType();
    }

    /**
     * Validate that the media fields are consistent
     */
    public boolean isValid() {
        // If it's a text-only question, it shouldn't have media
        if (questionType == QuestionType.TEXT) {
            return questionMediaUrl == null && questionMediaId == null && questionMediaType == null;
        }

        // If it has media type but no URL/ID, it's inconsistent
        if (questionMediaType != null && (questionMediaUrl == null && questionMediaId == null)) {
            return false;
        }

        // Check media type consistency
        return isMediaTypeConsistent();
    }
}