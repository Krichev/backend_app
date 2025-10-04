package com.my.challenger.entity;

import com.my.challenger.entity.enums.CompletionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @Column(name = "status", nullable = false, columnDefinition = "completion_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CompletionStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completionDate;

    @Column(name = "verified_at")
    private LocalDateTime verificationDate;

    @Column(name = "completion_data")
    private String verificationProof; // URL or metadata

    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}