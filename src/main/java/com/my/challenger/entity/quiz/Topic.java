package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.ValidationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "topics",
        indexes = {
                @Index(name = "idx_topic_name", columnList = "name"),
                @Index(name = "idx_topic_category", columnList = "category"),
                @Index(name = "idx_is_active", columnList = "is_active"),
                @Index(name = "idx_topics_parent_id", columnList = "parent_id"),
                @Index(name = "idx_topics_path", columnList = "path"),
                @Index(name = "idx_topics_validation_status", columnList = "validation_status"),
                @Index(name = "idx_topics_slug", columnList = "slug")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_topic_name", columnNames = {"name"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"creator", "parent", "children", "validatedBy"})
@ToString(exclude = {"creator", "parent", "children", "validatedBy"})
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 50)
    private String category; // e.g., "Science", "History", "Sports"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "question_count")
    @Builder.Default
    private Integer questionCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    // Hierarchical structure
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Topic parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Topic> children = new ArrayList<>();

    @Column(name = "path", length = 500)
    private String path;  // Materialized path: "/1/5/23/" for efficient tree queries

    @Column(name = "depth")
    @Builder.Default
    private Integer depth = 0;  // 0 = root, 1 = first level child, etc.

    // System and validation fields
    @Column(name = "is_system_topic")
    @Builder.Default
    private Boolean isSystemTopic = false;  // true for app-generated topics

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private ValidationStatus validationStatus = ValidationStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "slug", unique = true, length = 150)
    private String slug;  // URL-friendly: "geography-geology-minerals"

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Bidirectional relationship with QuizQuestion
//    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = false)
//    @Builder.Default
//    private List<QuizQuestion> quizQuestions = new ArrayList<>();

    // Helper method to increment question count
    public void incrementQuestionCount() {
        this.questionCount = (this.questionCount == null ? 0 : this.questionCount) + 1;
    }

    // Helper method to decrement question count
    public void decrementQuestionCount() {
        if (this.questionCount != null && this.questionCount > 0) {
            this.questionCount--;
        }
    }

    // Helper method to get child count
    public Integer getChildCount() {
        return children != null ? children.size() : 0;
    }

    // Helper method to check if this is a root topic
    public boolean isRootTopic() {
        return parent == null;
    }

    // Helper method to check if this has children
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}