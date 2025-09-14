// VerificationDetailsService.java
package com.my.challenger.service.impl;

import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.challenge.LocationCoordinates;
import com.my.challenger.entity.challenge.PhotoVerificationDetails;
import com.my.challenger.entity.challenge.VerificationDetails;
import com.my.challenger.repository.LocationCoordinatesRepository;
import com.my.challenger.repository.PhotoVerificationDetailsRepository;
import com.my.challenger.repository.VerificationDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VerificationDetailsService {

    private final VerificationDetailsRepository verificationDetailsRepository;
    private final LocationCoordinatesRepository locationCoordinatesRepository;
    private final PhotoVerificationDetailsRepository photoVerificationDetailsRepository;

    @Transactional
    public VerificationDetails createVerificationDetails(Challenge challenge, String activityType, Double targetValue, Double radius) {
        VerificationDetails verificationDetails = VerificationDetails.builder()
                .challenge(challenge)
                .activityType(activityType)
                .targetValue(targetValue)
                .radius(radius)
                .build();

        return verificationDetailsRepository.save(verificationDetails);
    }

    @Transactional
    public VerificationDetails createWithLocationCoordinates(Challenge challenge, String activityType, 
                                                           Double targetValue, Double radius,
                                                           Double latitude, Double longitude) {
        // Create location coordinates
        LocationCoordinates locationCoordinates = LocationCoordinates.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
        locationCoordinates = locationCoordinatesRepository.save(locationCoordinates);

        // Create verification details with location
        VerificationDetails verificationDetails = VerificationDetails.builder()
                .challenge(challenge)
                .activityType(activityType)
                .targetValue(targetValue)
                .radius(radius)
                .locationCoordinates(locationCoordinates)
                .build();

        return verificationDetailsRepository.save(verificationDetails);
    }

    @Transactional
    public VerificationDetails createWithPhotoDetails(Challenge challenge, String activityType, 
                                                     Double targetValue, Double radius,
                                                     String description, Boolean requiresPhotoComparison, 
                                                     String verificationMode) {
        // Create photo verification details
        PhotoVerificationDetails photoDetails = PhotoVerificationDetails.builder()
                .description(description)
                .requiresPhotoComparison(requiresPhotoComparison)
                .verificationMode(verificationMode)
                .build();
        photoDetails = photoVerificationDetailsRepository.save(photoDetails);

        // Create verification details with photo details
        VerificationDetails verificationDetails = VerificationDetails.builder()
                .challenge(challenge)
                .activityType(activityType)
                .targetValue(targetValue)
                .radius(radius)
                .photoDetails(photoDetails)
                .build();

        return verificationDetailsRepository.save(verificationDetails);
    }

    @Transactional
    public VerificationDetails createComplete(Challenge challenge, String activityType, Double targetValue, Double radius,
                                            Double latitude, Double longitude,
                                            String description, Boolean requiresPhotoComparison, String verificationMode) {
        // Create location coordinates
        LocationCoordinates locationCoordinates = null;
        if (latitude != null && longitude != null) {
            locationCoordinates = LocationCoordinates.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
            locationCoordinates = locationCoordinatesRepository.save(locationCoordinates);
        }

        // Create photo verification details
        PhotoVerificationDetails photoDetails = null;
        if (description != null || requiresPhotoComparison != null || verificationMode != null) {
            photoDetails = PhotoVerificationDetails.builder()
                    .description(description)
                    .requiresPhotoComparison(requiresPhotoComparison != null ? requiresPhotoComparison : false)
                    .verificationMode(verificationMode != null ? verificationMode : "standard")
                    .build();
            photoDetails = photoVerificationDetailsRepository.save(photoDetails);
        }

        // Create verification details
        VerificationDetails verificationDetails = VerificationDetails.builder()
                .challenge(challenge)
                .activityType(activityType)
                .targetValue(targetValue)
                .radius(radius)
                .locationCoordinates(locationCoordinates)
                .photoDetails(photoDetails)
                .build();

        return verificationDetailsRepository.save(verificationDetails);
    }

    @Transactional(readOnly = true)
    public List<VerificationDetails> findByChallengeId(Long challengeId) {
        return verificationDetailsRepository.findByChallengeId(challengeId);
    }

    @Transactional(readOnly = true)
    public Optional<VerificationDetails> findById(Long id) {
        return verificationDetailsRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<VerificationDetails> findByChallengeIdAndActivityType(Long challengeId, String activityType) {
        return verificationDetailsRepository.findByChallengeIdAndActivityType(challengeId, activityType);
    }

    @Transactional
    public VerificationDetails updateVerificationDetails(Long id, String activityType, Double targetValue, Double radius) {
        VerificationDetails verificationDetails = verificationDetailsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VerificationDetails not found with id: " + id));

        if (activityType != null) verificationDetails.setActivityType(activityType);
        if (targetValue != null) verificationDetails.setTargetValue(targetValue);
        if (radius != null) verificationDetails.setRadius(radius);

        return verificationDetailsRepository.save(verificationDetails);
    }

    @Transactional
    public void deleteVerificationDetails(Long id) {
        verificationDetailsRepository.deleteById(id);
    }

    @Transactional
    public void deleteByChallengeId(Long challengeId) {
        verificationDetailsRepository.deleteByChallengeId(challengeId);
    }

    @Transactional(readOnly = true)
    public boolean existsByChallengeId(Long challengeId) {
        return verificationDetailsRepository.existsByChallengeId(challengeId);
    }
}