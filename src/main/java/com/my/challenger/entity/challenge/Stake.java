package com.my.challenger.entity.challenge;

import com.my.challenger.entity.enums.CurrencyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stakes")
public class Stake {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, columnDefinition = "currency_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CurrencyType currency;

    @Column(name = "collective_pool")
    private boolean collectivePool;

    @Column(name = "challenge_id")
    Long challenge;
}
