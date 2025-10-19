package com.my.challenger.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    private String bio;

    // ========== POINTS SYSTEM ==========
    @Column(name = "points", nullable = false)
    private Long points = 0L;

    @Column(name = "total_points_earned")
    private Long totalPointsEarned = 0L;

    @Column(name = "total_points_spent")
    private Long totalPointsSpent = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "members")
    private Set<Group> groups = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "user_quests",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "quest_id")
    )
    private Set<Quest> participatingQuests = new HashSet<>();

    @OneToMany(mappedBy = "creator")
    private Set<Quest> createdQuests = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "reward_users",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "reward_id")
    )
    private Set<Reward> rewards = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (points == null) {
            points = 0L;
        }
        if (totalPointsEarned == null) {
            totalPointsEarned = 0L;
        }
        if (totalPointsSpent == null) {
            totalPointsSpent = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== HELPER METHODS ==========

    public void addPoints(Long amount) {
        this.points += amount;
        this.totalPointsEarned += amount;
    }

    public void deductPoints(Long amount) {
        if (this.points < amount) {
            throw new IllegalStateException("Insufficient points");
        }
        this.points -= amount;
        this.totalPointsSpent += amount;
    }

    public boolean hasEnoughPoints(Long required) {
        return this.points >= required;
    }
}