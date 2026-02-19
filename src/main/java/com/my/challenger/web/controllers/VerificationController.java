package com.my.challenger.web.controllers;

import com.my.challenger.dto.verification.LocationVerificationRequest;
import com.my.challenger.dto.verification.PhotoVerificationRequest;
import com.my.challenger.dto.verification.VerificationResponse;
import com.my.challenger.entity.User;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.impl.location.LocationVerificationService;
import com.my.challenger.service.impl.photo.PhotoVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Controller for handling verification requests for challenges
 */
@RestController
@RequestMapping("/verification")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final PhotoVerificationService photoVerificationService;
    private final LocationVerificationService locationVerificationService;
    private final UserRepository userRepository;


    @PostMapping(value = "/photo-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerificationResponse> verifyPhotoJson(
            @RequestBody @Valid PhotoVerificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("Received JSON photo verification request for challenge {}", request.getChallengeId());

            // Get user ID from authenticated user
            Long userId = getUserIdFromUserDetails(userDetails);

            // Process the base64 image directly
            Map<String, Object> result = processBase64Image(
                    request.getBase64Image(),
                    request.getFileName(),
                    request.getChallengeId(),
                    userId,
                    request.getPrompt(),
                    request.getAiPrompt());

            // Create response
            VerificationResponse response = VerificationResponse.builder()
                    .success(true)
                    .isVerified((Boolean) result.getOrDefault("isVerified", false))
                    .message((String) result.getOrDefault("message", "Photo verification processed"))
                    .details(result)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in JSON photo verification", e);
            return ResponseEntity.badRequest().body(
                    VerificationResponse.builder()
                            .success(false)
                            .isVerified(false)
                            .message("Error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Endpoint to verify a location for a challenge
     */
    @PostMapping("/location")
    public ResponseEntity<VerificationResponse> verifyLocation(
            @RequestBody @Valid LocationVerificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("Received location verification request for challenge {}", request.getChallengeId());

            // Get user ID from authenticated user
            Long userId = getUserIdFromUserDetails(userDetails);
            log.debug("Processing request for user ID: {}", userId);

            // Call the verification service
            Map<String, Object> result = locationVerificationService.verifyLocation(
                    request.getChallengeId(),
                    userId,
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getTimestamp());

            // Create response
            VerificationResponse response = VerificationResponse.builder()
                    .success(true)
                    .isVerified((Boolean) result.getOrDefault("isVerified", false))
                    .message((String) result.getOrDefault("message", "Location verification processed"))
                    .details(result)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in location verification", e);
            return ResponseEntity.badRequest().body(
                    VerificationResponse.builder()
                            .success(false)
                            .isVerified(false)
                            .message("Error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * API endpoint to check verification status of a task
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<VerificationResponse> getVerificationStatus(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("Checking verification status for task {}", taskId);

            // Get user ID from authenticated user
            Long userId = getUserIdFromUserDetails(userDetails);

            // This endpoint would typically check the latest task completion status
            // Implementation would depend on your TaskCompletionRepository
            // For now, we'll return a placeholder response

            VerificationResponse response = VerificationResponse.builder()
                    .success(true)
                    .isVerified(false) // This would come from your repository
                    .message("Verification pending")
                    .details(Map.of(
                            "taskId", taskId,
                            "userId", userId,
                            "status", "PENDING" // This would come from your repository
                    ))
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking verification status", e);
            return ResponseEntity.badRequest().body(
                    VerificationResponse.builder()
                            .success(false)
                            .isVerified(false)
                            .message("Error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Convert base64 encoded string to image bytes and process directly
     * instead of creating a mock MultipartFile
     */
    private Map<String, Object> processBase64Image(String base64Image, String fileName,
                                                   Long challengeId, Long userId,
                                                   String prompt, String aiPrompt) {
        if (base64Image == null) {
            throw new IllegalArgumentException("Base64 image data cannot be null");
        }

        try {
            // Remove data URL prefix if present
            String base64Data = base64Image;
            if (base64Image.contains(";base64,")) {
                base64Data = base64Image.split(";base64,")[1];
            } else if (base64Image.contains(",")) {
                base64Data = base64Image.split(",")[1];
            }

            // Trim and validate
            base64Data = base64Data.trim();
            if (base64Data.isEmpty()) {
                throw new IllegalArgumentException("Base64 image data is empty");
            }

            // Decode the base64 string
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);

            // Use a default filename if none is provided
            String finalFileName = (fileName != null) ? fileName : "image.jpg";
            String contentType = determineContentType(finalFileName);

            // Call the service method directly with the byte array instead of MultipartFile
            // This would require modifying the photoVerificationService to accept byte arrays
            return photoVerificationService.verifyPhotoFromBytes(
                    challengeId, userId, imageBytes, contentType, prompt, aiPrompt);
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 format", e);
            throw new IllegalArgumentException("Invalid base64 format: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing base64 image", e);
            throw new IllegalArgumentException("Error processing image: " + e.getMessage());
        }
    }

    /**
     * Determine content type based on file extension
     */
    private String determineContentType(String fileName) {
        if (fileName == null) {
            return "image/jpeg";
        }

        String lowercaseFileName = fileName.toLowerCase();
        if (lowercaseFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowercaseFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowercaseFileName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowercaseFileName.endsWith(".webp")) {
            return "image/webp";
        }

        return "image/jpeg"; // Default
    }

    /**
     * Extract user ID from UserDetails
     */
    private Long getUserIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        // This depends on your authentication setup
        // Assuming UserDetails object contains the username which maps to your User entity
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return user.getId();
    }
}