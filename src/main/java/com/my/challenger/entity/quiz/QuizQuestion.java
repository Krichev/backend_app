package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.enums.QuestionVisibility;
import com.my.challenger.entity.enums.QuizDifficulty;
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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions", indexes = {
        @Index(name = "idx_quiz_question_difficulty", columnList = "difficulty"),
        @Index(name = "idx_quiz_question_topic_id", columnList = "topic_id"),
        @Index(name = "idx_quiz_question_user_created", columnList = "is_user_created"),
        @Index(name = "idx_quiz_question_creator_id", columnList = "creator_id"),
        @Index(name = "idx_quiz_question_type", columnList = "question_type"),
        @Index(name = "idx_quiz_question_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(name = "legacy_question_id")
    private Integer legacyQuestionId;

    // Core question content
    @Column(name = "question", nullable = false, length = 1000)
    private String question;

    @Column(name = "answer", nullable = false, length = 500)
    private String answer;

    // Classification
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private QuizDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    @Builder.Default
    private QuestionType questionType = QuestionType.TEXT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "legacy_topic", length = 100)
    private String legacyTopic;

    @Column(name = "source", length = 100)
    private String source;

    // Enhanced metadata
    @Column(name = "authors", length = 200)
    private String authors;

    @Column(name = "comments", length = 500)
    private String comments;

    @Column(name = "pass_criteria", length = 200)
    private String passCriteria;

    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    // Media properties - NEW/ENHANCED FOR MEDIA SUPPORT
    @Column(name = "question_media_url", length = 500)
    private String questionMediaUrl;

    @Column(name = "question_media_id", length = 50)
    private String questionMediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_media_type")
    private MediaType questionMediaType;

    @Column(name = "question_thumbnail_url", length = 500)
    private String questionThumbnailUrl;

    // User creation tracking
    @Column(name = "is_user_created")
    @Builder.Default
    private Boolean isUserCreated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    // Status and usage
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Visibility setting for the question
     * Controls who can see and use this question
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, columnDefinition = "visibility_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private QuestionVisibility visibility = QuestionVisibility.QUIZ_ONLY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_quiz_id")
    private Challenge originalQuiz;

//    @OneToMany(mappedBy = "originalQuiz", cascade = CascadeType.ALL)
//    @Builder.Default
//    private List<Challenge> derivedQuestions = new ArrayList<>();

    // Helper methods for media handling
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

    public boolean hasVideoMedia() {
        return questionMediaType == MediaType.VIDEO;
    }

    public boolean hasImageMedia() {
        return questionMediaType == MediaType.IMAGE;
    }

    public boolean hasAudioMedia() {
        return questionMediaType == MediaType.AUDIO;
    }

    public boolean isMultimediaQuestion() {
        return questionType == QuestionType.MULTIMEDIA;
    }

    // Validation helper
    public boolean isMediaTypeConsistent() {
        if (questionType == QuestionType.TEXT) {
            return questionMediaType == null;
        }
        if (questionType == QuestionType.IMAGE) {
            return questionMediaType == MediaType.IMAGE ||
                    questionMediaType == MediaType.QUIZ_QUESTION;
        }
        if (questionType == QuestionType.AUDIO) {
            return questionMediaType == MediaType.AUDIO ||
                    questionMediaType == MediaType.QUIZ_QUESTION;
        }
        if (questionType == QuestionType.VIDEO) {
            return questionMediaType == MediaType.VIDEO ||
                    questionMediaType == MediaType.QUIZ_QUESTION;
        }
        if (questionType == QuestionType.MULTIMEDIA) {
            return questionMediaType != null;
        }
        return true;
    }

    // Auto-set media type based on question type
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

    public void autoSetMediaType() {
        this.questionMediaType = getExpectedMediaType();
    }

    // Validation
    public boolean isValid() {
        if (questionType == QuestionType.TEXT) {
            return questionMediaUrl == null &&
                    questionMediaId == null &&
                    questionMediaType == null;
        }
        if (questionMediaType != null &&
                (questionMediaUrl == null && questionMediaId == null)) {
            return false;
        }
        return isMediaTypeConsistent();
    }

    // Usage tracking
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }

    public void decrementUsageCount() {
        if (this.usageCount != null && this.usageCount > 0) {
            this.usageCount--;
        }
    }

    // Topic helper
    public String getTopicName() {
        return topic != null ? topic.getName() : legacyTopic;
    }

    // Creator helper
    public String getCreatorUsername() {
        return creator != null ? creator.getUsername() : null;
    }

    public Long getCreatorId() {
        return creator != null ? creator.getId() : null;
    }
}