package com.my.challenger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "app_version_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersionConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String platform;

    @Column(name = "min_supported_version", nullable = false)
    private String minSupportedVersion;

    @Column(name = "force_update_below")
    private String forceUpdateBelow;

    @Column(name = "github_owner", nullable = false)
    private String githubOwner;

    @Column(name = "github_repo", nullable = false)
    private String githubRepo;

    @Column(name = "cache_ttl_minutes", nullable = false)
    private Integer cacheTtlMinutes;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
