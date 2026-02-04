package com.my.challenger.entity.parental;

import com.my.challenger.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "parental_links")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentalLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_user_id", nullable = false)
    private User parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_user_id", nullable = false)
    private User child;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ParentalLinkStatus status = ParentalLinkStatus.PENDING;

    @Column(name = "verification_code", length = 10)
    private String verificationCode;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Boolean> permissions;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (permissions == null) {
            permissions = Map.of(
                "canSetBudget", true,
                "canViewActivity", true,
                "canApproveWagers", true,
                "canGrantTime", true
            );
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
