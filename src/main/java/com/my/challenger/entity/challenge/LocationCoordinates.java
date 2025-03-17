package com.my.challenger.entity.challenge;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class LocationCoordinates {
    private Double latitude;

    private Double longitude;
}
