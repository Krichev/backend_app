package com.my.challenger.dto.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationDetailsDto {
    private Long id;
    private String activityType;
    private Double targetValue;
    private Double radius;
    private Long challengeId;
    
    // Location coordinates
    private LocationCoordinatesDto locationCoordinates;
    
    // Photo verification details
    private PhotoVerificationDetailsDto photoDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationCoordinatesDto {
        private Long id;
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoVerificationDetailsDto {
        private Long id;
        private String description;
        private Boolean requiresPhotoComparison;
        private String verificationMode;
    }
}