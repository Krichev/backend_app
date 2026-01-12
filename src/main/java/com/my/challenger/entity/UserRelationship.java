package com.my.challenger.entity;

import com.my.challenger.entity.enums.RelationshipStatus;
import com.my.challenger.entity.enums.RelationshipType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_relationships", 
    indexes = {
        @Index(name = "idx_user_relationships_user", columnList = "user_id, status"),
        @Index(name = "idx_user_relationships_related_user", columnList = "related_user_id, status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_relationship", columnNames = {"user_id", "related_user_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "relatedUser"})
@ToString(exclude = {"user", "relatedUser"})
public class UserRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_user_relationships_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_user_relationships_related_user"))
    private User relatedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 50)
    private RelationshipType relationshipType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private RelationshipStatus status = RelationshipStatus.PENDING;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_favorite")
    @Builder.Default
    private Boolean isFavorite = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}