package com.my.challenger.entity.challenge;

import com.my.challenger.entity.ChallengeProgress;
import com.my.challenger.entity.Group;
import com.my.challenger.entity.Task;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "challenges")
@Getter
@Setter
public class Challenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ChallengeType type;

    private String title;

//    private String description;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @OneToMany(mappedBy = "challenge")
    private Set<Task> tasks = new HashSet<>();

//    @ManyToMany(mappedBy = "challenges")
//    private List<User> participants;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    private boolean isPublic;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private FrequencyType frequency;

    @Enumerated(EnumType.STRING)
    private VerificationMethod verificationMethod;

    @Embedded
    private VerificationDetails verificationDetails;

    @Embedded
    private Stake stake;

    @OneToMany(mappedBy = "challenge")
    private List<ChallengeProgress> progress;

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status;
}

//enum ChallengeType {
//    ACCOUNTABILITY, QUEST, EVENT
//}
//
//enum Frequency {
//    DAILY, WEEKLY, ONE_TIME
//}
//
//enum VerificationMethod {
//    ACTIVITY, PHOTO, LOCATION, MANUAL
//}
//
//enum ChallengeStatus {
//    PENDING, ACTIVE, COMPLETED, CANCELLED
//}

