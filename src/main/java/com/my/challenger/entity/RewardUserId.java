package com.my.challenger.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

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
