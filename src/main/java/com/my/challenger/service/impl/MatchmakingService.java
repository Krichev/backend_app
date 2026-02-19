package com.my.challenger.service.impl;

import com.my.challenger.dto.competitive.MatchmakingStatusDTO;
import com.my.challenger.entity.User;
import com.my.challenger.entity.competitive.CompetitiveMatch;
import com.my.challenger.entity.competitive.MatchmakingQueueEntry;
import com.my.challenger.entity.enums.AudioChallengeType;
import com.my.challenger.entity.enums.CompetitiveMatchStatus;
import com.my.challenger.entity.enums.CompetitiveMatchType;
import com.my.challenger.entity.enums.MatchmakingStatus;
import com.my.challenger.repository.CompetitiveMatchRepository;
import com.my.challenger.repository.MatchmakingQueueRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchmakingService {

    private final MatchmakingQueueRepository queueRepository;
    private final CompetitiveMatchRepository matchRepository;
    private final UserRepository userRepository;

    @Scheduled(fixedRate = 5000) // Every 5 seconds
    @SchedulerLock(name = "MatchmakingService_processQueue", lockAtLeastFor = "PT2S", lockAtMostFor = "PT4S")
    @Transactional
    public void processMatchmakingQueue() {
        // We iterate through challenge types to find matches
        // In a real production system, this would be more optimized (e.g. database query for pairs)
        for (AudioChallengeType type : AudioChallengeType.values()) {
            // Process common round counts
            processQueueForTypeAndRounds(type, 1);
            processQueueForTypeAndRounds(type, 3);
            processQueueForTypeAndRounds(type, 5);
        }
    }

    private void processQueueForTypeAndRounds(AudioChallengeType type, Integer rounds) {
        List<MatchmakingQueueEntry> entries = queueRepository
                .findByStatusAndAudioChallengeTypeAndPreferredRounds(
                        MatchmakingStatus.QUEUED, type, rounds);

        if (entries.size() < 2) {
            return;
        }

        log.info("Found {} players for {} ({} rounds)", entries.size(), type, rounds);

        // Simple FIFO matching
        for (int i = 0; i < entries.size() - 1; i += 2) {
            MatchmakingQueueEntry entry1 = entries.get(i);
            MatchmakingQueueEntry entry2 = entries.get(i + 1);

            createMatch(entry1, entry2, type, rounds);
        }
    }

    private void createMatch(MatchmakingQueueEntry entry1, MatchmakingQueueEntry entry2, 
                             AudioChallengeType type, Integer rounds) {
        log.debug("Matching users {} and {}", entry1.getUser().getId(), entry2.getUser().getId());

        // Create match
        CompetitiveMatch match = CompetitiveMatch.builder()
                .matchType(CompetitiveMatchType.RANDOM_MATCHMAKING)
                .status(CompetitiveMatchStatus.READY)
                .player1(entry1.getUser())
                .player2(entry2.getUser())
                .totalRounds(rounds)
                .currentRound(0)
                .audioChallengeType(type)
                .startedAt(null) // Starts when players acknowledge? Or immediately?
                                 // Instructions say "startMatch: Verify both players ready... Change status to IN_PROGRESS"
                                 // But for random match, they just joined. Let's set to READY.
                .build();

        match = matchRepository.save(match);

        // Update queue entries
        updateEntryMatched(entry1, entry2.getUser(), match);
        updateEntryMatched(entry2, entry1.getUser(), match);
    }

    private void updateEntryMatched(MatchmakingQueueEntry entry, User opponent, CompetitiveMatch match) {
        entry.setStatus(MatchmakingStatus.MATCHED);
        entry.setMatchedWithUser(opponent);
        entry.setMatchedMatch(match);
        entry.setMatchedAt(LocalDateTime.now());
        queueRepository.save(entry);
    }

    @Scheduled(fixedRate = 60000) // Every minute
    @SchedulerLock(name = "MatchmakingService_cleanupExpired", lockAtLeastFor = "PT30S", lockAtMostFor = "PT50S")
    @Transactional
    public void cleanupExpiredEntries() {
        int deletedCount = queueRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (deletedCount > 0) {
            log.debug("Cleaned up {} expired matchmaking entries", deletedCount);
        }
    }

    @Transactional(readOnly = true)
    public MatchmakingStatusDTO getQueueStatus(Long userId) {
        Optional<MatchmakingQueueEntry> entryOpt = queueRepository.findByUserId(userId);
        
        if (entryOpt.isEmpty()) {
            return null;
        }
        
        MatchmakingQueueEntry entry = entryOpt.get();
        
        // Calculate position (naive)
        int position = 0;
        if (entry.getStatus() == MatchmakingStatus.QUEUED) {
             // This is expensive, but for MVP acceptable
             List<MatchmakingQueueEntry> preceding = queueRepository
                     .findByStatusAndAudioChallengeTypeAndPreferredRounds(
                             MatchmakingStatus.QUEUED, 
                             entry.getAudioChallengeType(), 
                             entry.getPreferredRounds());
             
             for (MatchmakingQueueEntry e : preceding) {
                 position++;
                 if (e.getId().equals(entry.getId())) break;
             }
        }
        
        return MatchmakingStatusDTO.builder()
                .status(entry.getStatus().name())
                .queuedAt(entry.getQueuedAt())
                .audioChallengeType(entry.getAudioChallengeType().name())
                .preferredRounds(entry.getPreferredRounds())
                .queuePosition(position)
                .estimatedWaitSeconds(position * 10) // Mock estimation
                .build();
    }
    
    @Transactional(readOnly = true)
    public int getEstimatedQueuePosition(Long userId) {
         MatchmakingStatusDTO status = getQueueStatus(userId);
         return status != null ? status.getQueuePosition() : 0;
    }
}
