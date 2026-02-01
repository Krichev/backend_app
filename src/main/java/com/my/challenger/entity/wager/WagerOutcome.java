package com.my.challenger.entity.wager;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.SettlementType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wager_outcomes")
public class WagerOutcome {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wager_id", nullable = false)
    private Wager wager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loser_id")
    private User loser;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_type", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SettlementType settlementType;

    @Column(name = "amount_distributed", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountDistributed;

    @Column(name = "penalty_assigned", nullable = false)
    @Builder.Default
    private boolean penaltyAssigned = false;

    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "settled_at", updatable = false)
    private LocalDateTime settledAt;
}
