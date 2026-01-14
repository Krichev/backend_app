package com.my.challenger.service;

import com.my.challenger.dto.challenge.ChallengeAudioConfigDTO;
import com.my.challenger.dto.challenge.ChallengeAudioResponseDTO;

/**
 * Service interface for managing challenge audio configuration
 * Mirrors QuestAudioService for consistency
 */
public interface ChallengeAudioService {

    /**
     * Update audio configuration for a challenge
     */
    ChallengeAudioResponseDTO updateAudioConfig(Long challengeId, ChallengeAudioConfigDTO config);

    /**
     * Get audio configuration for a challenge
     */
    ChallengeAudioResponseDTO getAudioConfig(Long challengeId);

    /**
     * Remove audio configuration from a challenge
     */
    void removeAudioConfig(Long challengeId);
}