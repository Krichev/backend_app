package com.my.challenger.entity.enums;

public enum VerificationMethod {
    MANUAL,      // Manual verification by admin or challenge creator
    FITNESS_API, // Verification via fitness tracking API (e.g., Strava, Fitbit)
    PHOTO,       // Photo-based verification
    QUIZ,        // Quiz or question-based verification
    LOCATION,    // Location-based verification (GPS)
    NONE, ACTIVITY     // Activity tracking verification
}
