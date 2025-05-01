package com.my.challenger.entity.challenge;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "location_coordinates")
@Getter
@Setter
public class LocationCoordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double latitude;
    private Double longitude;

    // Relationship to VerificationDetails
    @OneToOne(mappedBy = "locationCoordinates")
    private VerificationDetails verificationDetails;
}
