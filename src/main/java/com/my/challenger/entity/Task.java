package com.my.challenger.entity;

import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.TaskStatus;
import com.my.challenger.entity.enums.TaskType;
import com.my.challenger.entity.enums.VerificationMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", nullable = false)
    private VerificationMethod verificationMethod;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedToUser;

    @Column(name = "assigned_to", updatable = false, insertable = false)
    private Long assignedTo;

    @ManyToOne
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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Builder method to create a quiz task
     */
    public static Task createQuizTask(Challenge challenge, User assignedUser) {
        return Task.builder()
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .type(challenge.getFrequency() != null ?
                        TaskType.valueOf(challenge.getFrequency().name()) : TaskType.ONE_TIME)
                .status(TaskStatus.NOT_STARTED)
                .verificationMethod(VerificationMethod.QUIZ) // Explicit quiz verification
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .challenge(challenge)
                .assignedToUser(assignedUser)
                .assignedTo(assignedUser.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}