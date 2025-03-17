package com.my.challenger.entity.challenge;

import com.my.challenger.entity.enums.CurrencyType;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Stake {
    private Double amount;

    private CurrencyType currency;

    private boolean collectivePool;
}
