package com.my.challenger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "screen_time_reset_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenTimeResetLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reset_date", nullable = false)
    private LocalDate resetDate;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "users_processed", nullable = false)
    private Integer usersProcessed = 0;

    @Column(name = "users_reset", nullable = false)
    private Integer usersReset = 0;

    @Column(name = "users_skipped", nullable = false)
    private Integer usersSkipped = 0;

    @Column(name = "users_failed", nullable = false)
    private Integer usersFailed = 0;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "instance_id", length = 100)
    private String instanceId;
}
