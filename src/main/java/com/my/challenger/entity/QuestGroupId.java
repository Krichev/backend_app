package com.my.challenger.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class QuestGroupId implements Serializable {

    private Long questId;
    private Long groupId;

    public QuestGroupId() {}

    public QuestGroupId(Long questId, Long groupId) {
        this.questId = questId;
        this.groupId = groupId;
    }

    // Getters, Setters, equals() and hashCode()
}
