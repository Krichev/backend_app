package com.my.challenger.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GroupUserId implements Serializable {

    private Long groupId;
    private Long userId;

    public GroupUserId() {}

    public GroupUserId(Long groupId, Long userId) {
        this.groupId = groupId;
        this.userId = userId;
    }

    // Getters, Setters, equals() and hashCode()
}
