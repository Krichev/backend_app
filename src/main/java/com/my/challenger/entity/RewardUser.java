package com.my.challenger.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "reward_users")
public class RewardUser {

    @EmbeddedId
    private RewardUserId id;

    @ManyToOne
    @MapsId("rewardId")
    @JoinColumn(name = "reward_id")
    private Reward reward;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    // Getters and Setters
}
