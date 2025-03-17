package com.my.challenger.entity;

import com.my.challenger.entity.enums.CurrencyType;
import com.my.challenger.entity.enums.RewardSource;
import com.my.challenger.entity.enums.RewardType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardType type;

    @Column(precision = 10, scale = 2)
    private BigDecimal monetaryValue;

    @Enumerated(EnumType.STRING)
    private CurrencyType currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_source", nullable = false)
    private RewardSource rewardSource;

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
