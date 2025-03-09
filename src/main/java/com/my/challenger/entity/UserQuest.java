package com.my.challenger.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_quests")
public class UserQuest {

    @EmbeddedId
    private UserQuestId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("questId")
    @JoinColumn(name = "quest_id")
    private Quest quest;

    // Getters and Setters
}
