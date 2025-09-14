package com.my.challenger.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
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
