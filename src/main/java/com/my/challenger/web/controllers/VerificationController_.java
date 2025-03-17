//package com.my.challenger.web.controllers;
//
//import com.my.challenger.dto.verification.LocationVerificationRequest;
//import com.my.challenger.dto.verification.VerificationResponse;
//import com.my.challenger.service.impl.LocationVerificationService;
//import com.my.challenger.service.impl.photo.PhotoVerificationService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/verification")
//@RequiredArgsConstructor
//@Slf4j
//public class VerificationController {
//
//    private final PhotoVerificationService photoVerificationService;
//    private final LocationVerificationService locationVerificationService;
//
//    /**
//     * Endpoint to verify a photo for a challenge
//     */
//    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<VerificationResponse> verifyPhoto(
//            @RequestParam("challengeId") Long challengeId,
//            @RequestParam("image") MultipartFile photo,
//            @RequestParam(value = "prompt", required = false) String prompt,
//            @RequestParam(value = "aiPrompt", required = false) String aiPrompt,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        try {
//            // Get user ID from authenticated user
//            Long userId = getUserIdFromUserDetails(userDetails);
//
//            // Call the verification service
//            Map<String, Object> result = photoVerificationService.verifyPhoto(
//                    challengeId, userId, photo, prompt, aiPrompt);
//
//            // Create response
//            VerificationResponse response = VerificationResponse.builder()
//                    .success(true)
//                    .isVerified((Boolean) result.getOrDefault("isVerified", false))
//                    .message((String) result.getOrDefault("message", "Photo verification processed"))
//                    .details(result)
//                    .build();
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("Error in photo verification", e);
//            return ResponseEntity.badRequest().body(
//                    VerificationResponse.builder()
//                            .success(false)
//                            .isVerified(false)
//                            .message("Error: " + e.getMessage())
//                            .build()
//            );
//        }
//    }
//
//    /**
//     * Endpoint to verify a location for a challenge
//     */
//    @PostMapping("/location")
//    public ResponseEntity<VerificationResponse> verifyLocation(
//            @RequestBody LocationVerificationRequest request,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        try {
//            // Get user ID from authenticated user
//            Long userId = getUserIdFromUserDetails(userDetails);
//
//            // Call the verification service
//            Map<String, Object> result = locationVerificationService.verifyLocation(
//                    request.getChallengeId(),
//                    userId,
//                    request.getLatitude(),
//                    request.getLongitude(),
//                    request.getTimestamp());
//
//            // Create response
//            VerificationResponse response = VerificationResponse.builder()
//                    .success(true)
//                    .isVerified((Boolean) result.getOrDefault("isVerified", false))
//                    .message((String) result.getOrDefault("message", "Location verification processed"))
//                    .details(result)
//                    .build();
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("Error in location verification", e);
//            return ResponseEntity.badRequest().body(
//                    VerificationResponse.builder()
//                            .success(false)
//                            .isVerified(false)
//                            .message("Error: " + e.getMessage())
//                            .build()
//            );
//        }
//    }
//
//    /**
//     * Extract user ID from UserDetails
//     */
//    private Long getUserIdFromUserDetails(UserDetails userDetails) {
//        // This implementation depends on your authentication setup
//        // For example, if using a custom UserDetails implementation that contains the user ID
////        if (userDetails instanceof CustomUserDetails) {
////            return ((CustomUserDetails) userDetails).getId();
////        }
////
//        // Otherwise, look up the user by username
//        // This is a simplified example - in a real application, you'd have a user service
//        return 1L; // Placeholder
//    }
//}