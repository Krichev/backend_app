package com.my.challenger.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_app_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_app_settings_user"))
    private User user;

    @Column(name = "language", length = 5, nullable = false)
    @Builder.Default
    private String language = "en";

    @Column(name = "theme", length = 10, nullable = false)
    @Builder.Default
    private String theme = "system";

    @Column(name = "notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean notificationsEnabled = true;

    @Column(name = "enable_ai_answer_validation")
    @Builder.Default
    private Boolean enableAiAnswerValidation = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
