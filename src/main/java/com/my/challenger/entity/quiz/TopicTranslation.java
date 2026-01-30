package com.my.challenger.entity.quiz;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "topic_translations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_topic_translation_lang", columnNames = {"topic_id", "language_code"})
        },
        indexes = {
                @Index(name = "idx_topic_translations_topic_id", columnList = "topic_id"),
                @Index(name = "idx_topic_translations_lang", columnList = "language_code")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "language_code", nullable = false, length = 5)
    private String languageCode; // 'en', 'ru', etc.

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
