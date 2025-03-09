package com.my.challenger.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "quest_groups")
public class QuestGroup {

    @EmbeddedId
    private QuestGroupId id;

    @ManyToOne
    @MapsId("questId")
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @ManyToOne
    @MapsId("groupId")
    @JoinColumn(name = "group_id")
    private Group group;

    // Getters and Setters
}
