package com.my.challenger.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RewardUserId implements Serializable {

    private Long rewardId;
    private Long userId;

    public RewardUserId() {}

    public RewardUserId(Long rewardId, Long userId) {
        this.rewardId = rewardId;
        this.userId = userId;
    }

    // Getters, Setters, equals() and hashCode()
}
