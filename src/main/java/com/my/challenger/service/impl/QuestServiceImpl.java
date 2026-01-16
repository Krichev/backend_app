package com.my.challenger.service.impl;

import com.my.challenger.entity.Quest;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.QuestRepository;
import com.my.challenger.service.QuestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of QuestService handling soft delete and active filtering
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestServiceImpl implements QuestService {

    private final QuestRepository questRepository;

    @Override
    @Transactional
    public void deleteQuest(Long questId, Long userId) {
        log.info("🗑️ Attempting to soft delete quest {} by user {}", questId, userId);

        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found with ID: " + questId));

        // Ownership validation
        if (!quest.getCreator().getId().equals(userId)) {
            log.warn("🚫 Unauthorized delete attempt: User {} tried to delete quest {} owned by user {}", 
                    userId, questId, quest.getCreator().getId());
            throw new AccessDeniedException("You are not authorized to delete this quest");
        }

        // Idempotency: only update if active
        if (Boolean.TRUE.equals(quest.getIsActive())) {
            quest.setIsActive(false);
            questRepository.save(quest);
            log.info("✅ Quest {} successfully soft deleted by creator {}", questId, userId);
        } else {
            log.info("ℹ️ Quest {} is already inactive, nothing to do", questId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Quest getActiveQuest(Long questId) {
        return questRepository.findByIdAndIsActiveTrue(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Active quest not found with ID: " + questId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quest> getActiveQuestsByCreator(Long creatorId) {
        return questRepository.findByCreatorIdAndIsActiveTrue(creatorId);
    }
}
