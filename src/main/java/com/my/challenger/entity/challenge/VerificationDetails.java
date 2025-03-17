package com.my.challenger.entity.challenge;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class VerificationDetails {
    private String activityType;

    private Double targetValue;

    @Embedded
    private LocationCoordinates locationCoordinates;

    @Embedded
    private PhotoVerificationDetails photoDetails;

    private Double radius;
}
