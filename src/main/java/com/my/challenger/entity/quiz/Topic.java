package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "topics",
        indexes = {
                @Index(name = "idx_topic_name", columnList = "name"),
                @Index(name = "idx_topic_category", columnList = "category"),
                @Index(name = "idx_is_active", columnList = "is_active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_topic_name", columnNames = {"name"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"quizQuestions", "creator"})
@ToString(exclude = {"quizQuestions", "creator"})
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Bidirectional relationship with QuizQuestion
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<QuizQuestion> quizQuestions = new ArrayList<>();

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
}