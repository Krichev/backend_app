package com.my.challenger.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

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

    private String description;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

//    @ManyToMany(mappedBy = "challenges")
//    private List<User> participants;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    private boolean isPublic;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

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

enum ChallengeType {
    ACCOUNTABILITY, QUEST, EVENT
}

enum Frequency {
    DAILY, WEEKLY, ONE_TIME
}

enum VerificationMethod {
    ACTIVITY, PHOTO, LOCATION, MANUAL
}

enum ChallengeStatus {
    PENDING, ACTIVE, COMPLETED, CANCELLED
}

@Embeddable
@Getter
@Setter
class VerificationDetails {
    private String activityType;

    private Double targetValue;

    @Embedded
    private LocationCoordinates locationCoordinates;

    private Double radius;
}

@Embeddable
@Getter
@Setter
class LocationCoordinates {
    private Double latitude;

    private Double longitude;
}

@Embeddable
@Getter
@Setter
class Stake {
    private Double amount;

    private String currency;

    private boolean collectivePool;
}
