package com.my.challenger.entity;

import com.my.challenger.entity.enums.CurrencyType;
import com.my.challenger.entity.enums.RewardSource;
import com.my.challenger.entity.enums.RewardType;
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
@Table(name = "rewards")
public class Reward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "reward_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private RewardType type;

    @Column(name = "monetary_value")
    private Double monetaryValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, columnDefinition = "currency_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CurrencyType currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_source", nullable = false, columnDefinition = "reward_source")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
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
