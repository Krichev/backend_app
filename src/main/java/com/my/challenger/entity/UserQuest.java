package com.my.challenger.entity;

import com.my.challenger.entity.enums.UserQuestStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

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

    @Column(name = "joined_at")
    private LocalDateTime joinDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserQuestStatus status;

    // Constructors, getter/setter methods
    // ...
}
