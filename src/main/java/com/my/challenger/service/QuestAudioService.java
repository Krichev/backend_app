package com.my.challenger.service;

import com.my.challenger.dto.quest.QuestAudioConfigDTO;
import com.my.challenger.dto.quest.QuestAudioResponseDTO;

/**
 * Service interface for managing quest audio configuration
 */
public interface QuestAudioService {

    /**
     * Update audio configuration for a quest
     *
     * @param questId ID of the quest to update
     * @param config Audio configuration details
     * @return Updated audio configuration with URLs and metadata
     * @throws com.my.challenger.exception.ResourceNotFoundException if quest not found
     * @throws com.my.challenger.exception.InvalidAudioSegmentException if segment configuration is invalid
     */
    QuestAudioResponseDTO updateAudioConfig(Long questId, QuestAudioConfigDTO config);

    /**
     * Get audio configuration for a quest
     *
     * @param questId ID of the quest
     * @return Audio configuration with URLs and metadata, or null if no audio configured
     * @throws com.my.challenger.exception.ResourceNotFoundException if quest not found
     */
    QuestAudioResponseDTO getAudioConfig(Long questId);

    /**
     * Remove audio configuration from a quest
     *
     * @param questId ID of the quest
     * @throws com.my.challenger.exception.ResourceNotFoundException if quest not found
     */
    void removeAudioConfig(Long questId);
}
