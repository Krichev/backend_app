package com.my.challenger.entity;

import com.my.challenger.entity.enums.UserQuestStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @Column(name = "status", nullable = false, columnDefinition = "quiz_session_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserQuestStatus status;

    // Constructors, getter/setter methods
    // ...
}
