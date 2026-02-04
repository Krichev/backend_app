package com.my.challenger.entity.parental;

import com.my.challenger.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "parental_approvals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentalApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_user_id", nullable = false)
    private User parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_user_id", nullable = false)
    private User child;

    @Column(name = "approval_type", nullable = false, length = 30)
    private String approvalType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_details", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> requestDetails;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parent_response", columnDefinition = "jsonb")
    private Map<String, Object> parentResponse;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (requestDetails == null) requestDetails = Map.of();
    }
}
