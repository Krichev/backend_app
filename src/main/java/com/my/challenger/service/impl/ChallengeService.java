package com.my.challenger.service.impl;

import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.dto.CreateChallengeRequest;
import com.my.challenger.dto.UpdateChallengeRequest;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for Challenge operations
 */
public interface ChallengeService {

    /**
     * Get challenges with optional filters
     */
    List<ChallengeDTO> getChallenges(Map<String, Object> filters);

    /**
     * Get a specific challenge by ID
     */
    ChallengeDTO getChallengeById(Long id, Long requestUserId);

    /**
     * Get a specific challenge by ID
     */
    Optional<Challenge> getChallengeById(Long id);

    /**
     * Create a new challenge
     */
    ChallengeDTO createChallenge(CreateChallengeRequest request, Long creatorId);

    /**
     * Update an existing challenge
     */
    ChallengeDTO updateChallenge(Long id, UpdateChallengeRequest request, Long creatorId);

    /**
     * Delete a challenge
     */
    void deleteChallenge(Long id);

    /**
     * Join a challenge
     */
    void joinChallenge(Long challengeId, Long userId);

    /**
     * Submit challenge completion
     */
    void submitChallengeCompletion(Long challengeId, Long userId, Map<String, Object> proofData, String notes);

    /**
     * Verify/approve challenge completion
     */
    void verifyChallengeCompletion(Long challengeId, Long userId, boolean approved);

    /**
     * Search challenges by keyword
     */
    List<ChallengeDTO> searchChallenges(String query, Long requestUserId);

    /**
     * Get verification history for a challenge
     */
    List<Map<String, Object>> getVerificationHistory(Long challengeId, Long userId);

    /**
     * Validate if user has permission to modify the challenge
     */
    void validateChallengeOwnership(Long challengeId, Long userId);

    /**
     * Validate if user has permission to verify challenge completions
     */
    void validateChallengeVerificationRights(Long challengeId, Long userId);

    List<ChallengeDTO> getAccessibleChallenges(Long userId, Pageable pageable);

    List<ChallengeDTO> searchChallenges(String keyword, Long userId, Pageable pageable);

    void revokeAccess(Long challengeId, Long userId, Long revokedBy);

    void grantAccessToUsers(Challenge challenge, List<Long> userIds, User grantedBy);

    /**
     * Get access control list for a challenge
     * Returns list of users/groups with their access levels
     */
    List<Map<String, Object>> getAccessList(Long challengeId);
}