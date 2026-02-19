package com.my.challenger.service.impl;

import com.my.challenger.dto.CreateVerificationDetailsRequest;
import com.my.challenger.dto.verification.VerificationDetailsDto;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.challenge.VerificationDetails;
import com.my.challenger.service.ChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/verification-details")
@RequiredArgsConstructor
@Tag(name = "Verification Details", description = "API for managing verification details")
public class VerificationDetailsController {

    private final VerificationDetailsService verificationDetailsService;
    private final ChallengeService challengeService;

    @PostMapping
    @Operation(summary = "Create verification details")
    @ApiResponse(responseCode = "201", description = "Verification details created successfully")
    public ResponseEntity<VerificationDetailsDto> createVerificationDetails(
            @Valid @RequestBody CreateVerificationDetailsRequest request) {
        
        Challenge challenge = challengeService.getChallengeById(request.getChallengeId())
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        VerificationDetails verificationDetails = verificationDetailsService.createComplete(
                challenge,
                request.getActivityType(),
                request.getTargetValue(),
                request.getRadius(),
                request.getLatitude(),
                request.getLongitude(),
                request.getDescription(),
                request.getRequiresPhotoComparison(),
                request.getVerificationMode()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(convertToDto(verificationDetails));
    }

    @GetMapping("/challenge/{challengeId}")
    @Operation(summary = "Get verification details by challenge ID")
    public ResponseEntity<List<VerificationDetailsDto>> getVerificationDetailsByChallenge(
            @PathVariable Long challengeId) {
        
        List<VerificationDetails> verificationDetailsList = verificationDetailsService.findByChallengeId(challengeId);
        List<VerificationDetailsDto> dtoList = verificationDetailsList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get verification details by ID")
    public ResponseEntity<VerificationDetailsDto> getVerificationDetailsById(@PathVariable Long id) {
        VerificationDetails verificationDetails = verificationDetailsService.findById(id)
                .orElseThrow(() -> new RuntimeException("VerificationDetails not found"));

        return ResponseEntity.ok(convertToDto(verificationDetails));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update verification details")
    public ResponseEntity<VerificationDetailsDto> updateVerificationDetails(
            @PathVariable Long id,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) Double targetValue,
            @RequestParam(required = false) Double radius) {

        VerificationDetails updated = verificationDetailsService.updateVerificationDetails(
                id, activityType, targetValue, radius);

        return ResponseEntity.ok(convertToDto(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete verification details")
    public ResponseEntity<Void> deleteVerificationDetails(@PathVariable Long id) {
        verificationDetailsService.deleteVerificationDetails(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/challenge/{challengeId}")
    @Operation(summary = "Delete all verification details for a challenge")
    public ResponseEntity<Void> deleteVerificationDetailsByChallenge(@PathVariable Long challengeId) {
        verificationDetailsService.deleteByChallengeId(challengeId);
        return ResponseEntity.noContent().build();
    }

    private VerificationDetailsDto convertToDto(VerificationDetails verificationDetails) {
        VerificationDetailsDto.VerificationDetailsDtoBuilder builder = VerificationDetailsDto.builder()
                .id(verificationDetails.getId())
                .activityType(verificationDetails.getActivityType())
                .targetValue(verificationDetails.getTargetValue())
                .radius(verificationDetails.getRadius())
                .challengeId(verificationDetails.getChallenge().getId());

        // Convert location coordinates if present
        if (verificationDetails.getLocationCoordinates() != null) {
            builder.locationCoordinates(VerificationDetailsDto.LocationCoordinatesDto.builder()
                    .id(verificationDetails.getLocationCoordinates().getId())
                    .latitude(verificationDetails.getLocationCoordinates().getLatitude())
                    .longitude(verificationDetails.getLocationCoordinates().getLongitude())
                    .build());
        }

        // Convert photo details if present
        if (verificationDetails.getPhotoDetails() != null) {
            builder.photoDetails(VerificationDetailsDto.PhotoVerificationDetailsDto.builder()
                    .id(verificationDetails.getPhotoDetails().getId())
                    .description(verificationDetails.getPhotoDetails().getDescription())
                    .requiresPhotoComparison(verificationDetails.getPhotoDetails().getRequiresPhotoComparison())
                    .verificationMode(verificationDetails.getPhotoDetails().getVerificationMode())
                    .build());
        }

        return builder.build();
    }
}