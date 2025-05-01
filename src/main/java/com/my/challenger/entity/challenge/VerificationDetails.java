package com.my.challenger.entity.challenge;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "verification_details")
public class VerificationDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String activityType;

    private Double targetValue;

    @OneToOne
    @JoinColumn(name = "location_coordinates_id")
    private LocationCoordinates locationCoordinates;

    @OneToOne(mappedBy = "photo_details_id")
    private PhotoVerificationDetails photoDetails;

    private Double radius;
}
