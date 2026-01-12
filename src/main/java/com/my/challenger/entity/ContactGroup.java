package com.my.challenger.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "contact_groups",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_contact_group_name", columnNames = {"user_id", "name"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "relationships"})
@ToString(exclude = {"user", "relationships"})
public class ContactGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_contact_groups_user"))
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "icon", length = 50)
    private String icon;

    @ManyToMany
    @JoinTable(
        name = "contact_group_members",
        joinColumns = @JoinColumn(name = "contact_group_id"),
        inverseJoinColumns = @JoinColumn(name = "relationship_id")
    )
    @Builder.Default
    private Set<UserRelationship> relationships = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
