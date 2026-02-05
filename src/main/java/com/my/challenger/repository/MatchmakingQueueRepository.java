package com.my.challenger.repository;

import com.my.challenger.entity.competitive.MatchmakingQueueEntry;
import com.my.challenger.entity.enums.AudioChallengeType;
import com.my.challenger.entity.enums.MatchmakingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchmakingQueueRepository extends JpaRepository<MatchmakingQueueEntry, Long> {

    List<MatchmakingQueueEntry> findByStatusAndAudioChallengeTypeAndPreferredRounds(
            MatchmakingStatus status, 
            AudioChallengeType audioChallengeType, 
            Integer preferredRounds
    );

    Optional<MatchmakingQueueEntry> findByUserId(Long userId);

    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime expiryTime);
}
