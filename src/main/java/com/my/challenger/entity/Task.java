package com.my.challenger.entity;

import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.FrequencyType;
import com.my.challenger.entity.enums.TaskStatus;
import com.my.challenger.entity.enums.TaskType;
import com.my.challenger.entity.enums.VerificationMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Task entity with proper PostgreSQL ENUM type handling
 *
 * IMPORTANT: All enum fields use @JdbcTypeCode(SqlTypes.NAMED_ENUM)
 * to ensure proper PostgreSQL ENUM type mapping
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Task type - properly mapped to PostgreSQL ENUM
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "task_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TaskType type;

    /**
     * Task status - properly mapped to PostgreSQL ENUM
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "task_status_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TaskStatus status;

    /**
     * Verification method - properly mapped to PostgreSQL ENUM
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", nullable = false, columnDefinition = "verification_method_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerificationMethod verificationMethod;

    /**
     * Frequency type - properly mapped to PostgreSQL ENUM (optional field)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", columnDefinition = "frequency_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private FrequencyType frequency;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedToUser;

    @Column(name = "assigned_to", updatable = false, insertable = false)
    private Long assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TaskCompletion> completions = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // SAFETY CHECK: Ensure verification method is set
        if (verificationMethod == null) {
            // If challenge is set and has verification method, use it
            if (challenge != null && challenge.getVerificationMethod() != null) {
                verificationMethod = challenge.getVerificationMethod();
            } else {
                // Default fallback
                verificationMethod = VerificationMethod.MANUAL;
            }
        }

        // SAFETY CHECK: Ensure status is set
        if (status == null) {
            status = TaskStatus.NOT_STARTED;
        }

        // SAFETY CHECK: Ensure type is set
        if (type == null) {
            type = TaskType.ONE_TIME;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Builder method to create a quiz task with proper enum handling
     */
    public static Task createQuizTask(Challenge challenge, User assignedUser) {
        return Task.builder()
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .type(challenge.getFrequency() != null ?
                        mapFrequencyToTaskType(challenge.getFrequency()) : TaskType.ONE_TIME)
                .status(TaskStatus.NOT_STARTED)
                .verificationMethod(VerificationMethod.QUIZ) // Explicit quiz verification
                .frequency(challenge.getFrequency())
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .challenge(challenge)
                .assignedToUser(assignedUser)
                .assignedTo(assignedUser.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Helper method to map FrequencyType to TaskType
     */
    private static TaskType mapFrequencyToTaskType(FrequencyType frequency) {
        return switch (frequency) {
            case DAILY -> TaskType.DAILY;
            case WEEKLY -> TaskType.WEEKLY;
            case MONTHLY -> TaskType.MONTHLY;
            case ONE_TIME -> TaskType.ONE_TIME;
            default -> TaskType.ONE_TIME;
        };
    }

    /**
     * Check if task is overdue
     */
    public boolean isOverdue() {
        return endDate != null &&
                LocalDateTime.now().isAfter(endDate) &&
                status != TaskStatus.COMPLETED &&
                status != TaskStatus.VERIFIED;
    }

    /**
     * Check if task is completed
     */
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED || status == TaskStatus.VERIFIED;
    }

    /**
     * Check if task requires verification
     */
    public boolean requiresVerification() {
        return verificationMethod != null &&
                verificationMethod != VerificationMethod.MANUAL &&
                verificationMethod != VerificationMethod.NONE;
    }
}