package com.my.challenger.entity.challenge;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "photo_verification_details")
@Getter
@Setter
public class PhotoVerificationDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;               // Description of what should be in the photo
    private Boolean requiresPhotoComparison;  // Whether to compare with previous photos
    private String verificationMode;          // Verification mode (standard, strict, etc.)

    // Default constructor
    public PhotoVerificationDetails() {
        this.requiresPhotoComparison = false;
        this.verificationMode = "standard";
    }

    // Relationship to VerificationDetails
    @OneToOne(mappedBy = "photoDetails")
    private VerificationDetails verificationDetails;
}