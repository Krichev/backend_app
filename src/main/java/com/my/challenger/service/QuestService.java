package com.my.challenger.service;

import com.my.challenger.entity.Quest;
import java.util.List;

/**
 * Service interface for managing Quest lifecycle
 */
public interface QuestService {

    /**
     * Soft delete a quest with ownership validation
     * @param questId ID of the quest to delete
     * @param userId ID of the user requesting deletion
     */
    void deleteQuest(Long questId, Long userId);

    /**
     * Get an active quest by ID
     * @param questId ID of the quest
     * @return The active quest
     * @throws com.my.challenger.exception.ResourceNotFoundException if not found or inactive
     */
    Quest getActiveQuest(Long questId);

    /**
     * Get all active quests created by a specific user
     * @param creatorId ID of the creator
     * @return List of active quests
     */
    List<Quest> getActiveQuestsByCreator(Long creatorId);
}
