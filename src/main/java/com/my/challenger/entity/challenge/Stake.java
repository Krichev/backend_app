package com.my.challenger.entity.challenge;

import com.my.challenger.entity.enums.CurrencyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private CurrencyType currency;

    private boolean collectivePool;

    @Column(name = "challenge_id")
    Long challengeId;
}
