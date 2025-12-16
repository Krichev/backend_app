package com.my.challenger.repository;

import com.my.challenger.entity.Quest;
import com.my.challenger.entity.enums.QuestStatus;
import com.my.challenger.entity.enums.QuestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestRepository extends JpaRepository<Quest, Long> {

    /**
     * Find quests by creator
     */
    List<Quest> findByCreatorId(Long creatorId);

    /**
     * Find quests by type
     */
    List<Quest> findByType(QuestType type);

    /**
     * Find quests by status
     */
    List<Quest> findByStatus(QuestStatus status);

    /**
     * Find quests with audio configured
     */
    @Query("SELECT q FROM Quest q WHERE q.audioMedia IS NOT NULL")
    List<Quest> findQuestsWithAudio();

    /**
     * Find quest by ID with audio media eagerly loaded
     */
    @Query("SELECT q FROM Quest q LEFT JOIN FETCH q.audioMedia WHERE q.id = :questId")
    Optional<Quest> findByIdWithAudioMedia(@Param("questId") Long questId);
}
