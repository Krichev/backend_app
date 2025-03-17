package com.my.challenger.entity.challenge;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Embeddable class to store photo verification details
 */
@Embeddable
@Getter
@Setter
public class PhotoVerificationDetails {
    private String description;               // Description of what should be in the photo
    private Boolean requiresPhotoComparison;  // Whether to compare with previous photos
    private String verificationMode;          // Verification mode (standard, strict, etc.)
    
    // Default constructor
    public PhotoVerificationDetails() {
        this.requiresPhotoComparison = false;
        this.verificationMode = "standard";
    }
}