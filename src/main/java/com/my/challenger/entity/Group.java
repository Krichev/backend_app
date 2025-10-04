package com.my.challenger.entity;

import com.my.challenger.entity.enums.GroupType;
import com.my.challenger.entity.enums.PrivacySetting;
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

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToMany
    @JoinTable(
            name = "group_users",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();

    @ManyToMany(mappedBy = "groups")
    private Set<Quest> quests = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "group_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GroupType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_setting", nullable = false, columnDefinition = "privacy_setting")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PrivacySetting privacySetting;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}