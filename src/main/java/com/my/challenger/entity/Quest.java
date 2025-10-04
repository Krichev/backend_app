package com.my.challenger.entity;// Quest.java

import com.my.challenger.entity.enums.QuestStatus;
import com.my.challenger.entity.enums.QuestType;
import com.my.challenger.entity.enums.VisibilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
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
@Table(name = "quests")
public class Quest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "quest_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QuestType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, columnDefinition = "quest_visibility")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VisibilityType visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "quest_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QuestStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Task> tasks = new HashSet<>();

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reward> rewards = new HashSet<>();

    @ManyToMany(mappedBy = "participatingQuests")
    private Set<User> participants = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "quest_groups",
            joinColumns = @JoinColumn(name = "quest_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Group> groups = new HashSet<>();

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
