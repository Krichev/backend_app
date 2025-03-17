package com.my.challenger.entity;

import com.my.challenger.entity.enums.CompletionStatus;
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
@Table(name = "task_completions")
public class TaskCompletion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "task_id", insertable = false, updatable = false)
    private Long taskId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompletionStatus status;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @Column(name = "verification_date")
    private LocalDateTime verificationDate;

    @Column(name = "verification_proof")
    private String verificationProof; // URL or metadata

    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}