package com.my.challenger.entity.challenge;

import com.my.challenger.entity.ChallengeProgress;
import com.my.challenger.entity.Group;
import com.my.challenger.entity.Task;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.ChallengeStatus;
import com.my.challenger.entity.enums.ChallengeType;
import com.my.challenger.entity.enums.FrequencyType;
import com.my.challenger.entity.enums.VerificationMethod;
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

    @Column(name = "title")
    private String title;

    @Column(name = "description", updatable = false, insertable = false)
    private String description;

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

    @Column(name = "is_public")
    private boolean isPublic;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private FrequencyType frequency;

    @Enumerated(EnumType.STRING)
    private VerificationMethod verificationMethod;

    @OneToMany(mappedBy = "challenge")
    private List<VerificationDetails> verificationDetails;

    @OneToMany(mappedBy = "challenge")
    private List<Stake> stake;

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

