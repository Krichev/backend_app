package com.my.challenger.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class UserQuestId implements Serializable {

    private Long userId;
    private Long questId;

    public UserQuestId() {}

    public UserQuestId(Long userId, Long questId) {
        this.userId = userId;
        this.questId = questId;
    }

    // Getters, Setters, equals() and hashCode()
}
