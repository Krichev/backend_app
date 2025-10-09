package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions", indexes = {
        @Index(name = "idx_topic_id", columnList = "topic_id"),  // Changed from topic to topic_id
        @Index(name = "idx_difficulty", columnList = "difficulty"),
        @Index(name = "idx_question_type", columnList = "question_type"),
        @Index(name = "idx_external_id", columnList = "external_id"),
        @Index(name = "idx_legacy_question_id", columnList = "legacy_question_id"),
        @Index(name = "idx_is_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"tournamentQuestions", "creator", "topic"})
@ToString(exclude = {"tournamentQuestions", "creator", "topic"})
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, columnDefinition = "quiz_difficulty")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private QuizDifficulty difficulty = QuizDifficulty.MEDIUM;

    // CHANGED: From String topic to ManyToOne relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", foreignKey = @ForeignKey(name = "fk_quiz_question_topic"))
    private Topic topic;

    // Keep legacy topic for migration purposes (optional)
    @Column(name = "legacy_topic", length = 100)
    private String legacyTopic;

    @Column(length = 100)
    private String source;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(name = "is_user_created", nullable = false)
    @Builder.Default
    private Boolean isUserCreated = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "legacy_question_id")
    private Integer legacyQuestionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    @Builder.Default
    private QuestionType questionType = QuestionType.TEXT;

    @Column(name = "question_media_url", length = 500)
    private String questionMediaUrl;

    @Column(name = "question_media_id", length = 100)
    private String questionMediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_media_type", columnDefinition = "media_type_enum")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MediaType questionMediaType;

    @Column(name = "question_thumbnail_url", length = 500)
    private String questionThumbnailUrl;

    @Column(name = "authors", columnDefinition = "TEXT")
    private String authors;

    @Column(name = "pass_criteria", columnDefinition = "TEXT")
    private String passCriteria;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "quizQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Question> tournamentQuestions = new ArrayList<>();

    // =============== HELPER METHODS ===============


    // Helper method to get topic name safely
    public String getTopicName() {
        return topic != null ? topic.getName() : legacyTopic;
    }

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

    public boolean hasDocumentMedia() {
        return questionMediaType == MediaType.DOCUMENT;
    }

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

    public MediaType getExpectedMediaType() {
        switch (questionType) {
            case IMAGE: return MediaType.IMAGE;
            case AUDIO: return MediaType.AUDIO;
            case VIDEO: return MediaType.VIDEO;
            case MULTIMEDIA: return MediaType.QUIZ_QUESTION;
            case TEXT:
            default: return null;
        }
    }

    public void autoSetMediaType() {
        this.questionMediaType = getExpectedMediaType();
    }

    public boolean isValid() {
        if (questionType == QuestionType.TEXT) {
            return questionMediaUrl == null && questionMediaId == null && questionMediaType == null;
        }
        if (questionMediaType != null && (questionMediaUrl == null && questionMediaId == null)) {
            return false;
        }
        return isMediaTypeConsistent();
    }

    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }

    public void decrementUsageCount() {
        if (this.usageCount != null && this.usageCount > 0) {
            this.usageCount--;
        }
    }
}