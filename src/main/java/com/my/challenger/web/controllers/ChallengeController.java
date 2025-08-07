package com.my.challenger.web.controllers;

import com.my.challenger.dto.*;
import com.my.challenger.entity.User;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.impl.ChallengeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling challenge-related endpoints
 */
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
@Slf4j
public class ChallengeController {

    private final ChallengeService challengeService;
    private final UserRepository userRepository;

    /**
     * Get all challenges with optional filtering
     */
    @GetMapping
    public ResponseEntity<List<ChallengeDTO>> getAllChallenges(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long creator_id,
            @RequestParam(required = false) String targetGroup,
            @RequestParam(required = false) Long participant_id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Getting challenges with filters: page={}, limit={}, type={}, visibility={}, status={}, creator_id={}, targetGroup={}, participant_id={}",
                page, limit, type, visibility, status, creator_id, targetGroup, participant_id);
        User user = getUserFromUserDetails(userDetails);

        Map<String, Object> filters = new HashMap<>();
        filters.put("requestUserId", user.getId());
        filters.put("page", page != null ? page : 0);
        filters.put("limit", limit != null ? limit : 20);
        if (type != null) filters.put("type", type);
        if (visibility != null) filters.put("visibility", visibility);
        if (status != null) filters.put("status", status);
        if (creator_id != null) filters.put("creator_id", creator_id);
        if (targetGroup != null) filters.put("targetGroup", targetGroup);
        if (participant_id != null) filters.put("participant_id", participant_id);

        filters = Map.copyOf(filters);

        List<ChallengeDTO> challenges = challengeService.getChallenges(filters);
        return ResponseEntity.ok(challenges);
    }

    /**
     * Get a specific challenge by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChallengeDTO> getChallengeById(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting challenge by ID: {}", id);
        User user = getUserFromUserDetails(userDetails);
        ChallengeDTO challenge = challengeService.getChallengeById(id, user.getId());
        return ResponseEntity.ok(challenge);
    }

    /**
     * Create a new challenge
     */
    @PostMapping
    public ResponseEntity<ChallengeDTO> createChallenge(
            @Valid @RequestBody CreateChallengeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Creating new challenge: {}", request.getTitle());
        User user = getUserFromUserDetails(userDetails);

        ChallengeDTO createdChallenge = challengeService.createChallenge(request, user.getId());
        return ResponseEntity.ok(createdChallenge);
    }

    /**
     * Update an existing challenge
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ChallengeDTO> updateChallenge(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChallengeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating challenge ID: {}", id);
        User user = getUserFromUserDetails(userDetails);

        // Check if user is the creator or has admin rights
        challengeService.validateChallengeOwnership(id, user.getId());

        ChallengeDTO updatedChallenge = challengeService.updateChallenge(id, request, user.getId());
        return ResponseEntity.ok(updatedChallenge);
    }

    /**
     * Delete a challenge
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteChallenge(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Deleting challenge ID: {}", id);
        User user = getUserFromUserDetails(userDetails);

        // Check if user is the creator or has admin rights
        challengeService.validateChallengeOwnership(id, user.getId());

        challengeService.deleteChallenge(id);
        return ResponseEntity.ok(new MessageResponse("Challenge deleted successfully"));
    }

    /**
     * Search challenges by keyword
     */
    @GetMapping("/search")
    public ResponseEntity<List<ChallengeDTO>> searchChallenges(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Searching challenges with query: {}", q);
        User user = getUserFromUserDetails(userDetails);

        List<ChallengeDTO> challenges = challengeService.searchChallenges(q, user.getId());
        return ResponseEntity.ok(challenges);
    }

    /**
     * Join a challenge
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<MessageResponse> joinChallenge(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Joining challenge ID: {}", id);
        User user = getUserFromUserDetails(userDetails);

        challengeService.joinChallenge(id, user.getId());
        return ResponseEntity.ok(new MessageResponse("Successfully joined challenge"));
    }

    /**
     * Submit challenge completion/proof
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<MessageResponse> submitChallengeCompletion(
            @PathVariable Long id,
            @RequestBody(required = false) ChallengeCompletionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Submitting challenge completion for challenge ID: {}", id);
        User user = getUserFromUserDetails(userDetails);

        Map<String, Object> proofData = request != null ? request.getVerificationData() : null;
        String notes = request != null ? request.getNotes() : null;

        challengeService.submitChallengeCompletion(id, user.getId(), proofData, notes);
        return ResponseEntity.ok(new MessageResponse("Challenge completion submitted"));
    }

    /**
     * Verify/approve challenge completion (for admins/creators)
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<MessageResponse> verifyChallengeCompletion(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(request.get("userId").toString());
        boolean approved = Boolean.parseBoolean(request.get("approved").toString());

        log.info("Verifying challenge completion for challenge ID: {}, user ID: {}, approved: {}",
                id, userId, approved);

        User user = getUserFromUserDetails(userDetails);

        // Check if user is the creator or has admin rights
        challengeService.validateChallengeVerificationRights(id, user.getId());

        challengeService.verifyChallengeCompletion(id, userId, approved);
        return ResponseEntity.ok(new MessageResponse("Challenge verification updated"));
    }

    /**
     * Get verification history for a challenge
     */
    @GetMapping("/{id}/verifications")
    public ResponseEntity<List<Map<String, Object>>> getVerificationHistory(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Getting verification history for challenge ID: {}, user ID: {}", id, userId);
        User user = getUserFromUserDetails(userDetails);

        // If userId is not provided, use the authenticated user's ID
        Long targetUserId = userId != null ? userId : user.getId();

        List<Map<String, Object>> history = challengeService.getVerificationHistory(id, targetUserId);
        return ResponseEntity.ok(history);
    }

    /**
     * Helper method to get User from UserDetails
     */
    private User getUserFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}