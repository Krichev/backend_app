package com.my.challenger.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rewards")
public class Reward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    private String type; // MONETARY, POINTS, BADGE, CUSTOM

    @Column(precision = 10, scale = 2)
    private BigDecimal monetaryValue;

    private String currency;

    @Column(name = "reward_source")
    private String rewardSource; // INDIVIDUAL, GROUP, SYSTEM

    @ManyToOne
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @ManyToMany(mappedBy = "rewards")
    private Set<User> recipients = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
