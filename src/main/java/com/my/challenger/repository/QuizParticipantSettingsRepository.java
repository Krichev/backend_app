package com.my.challenger.repository;

import com.my.challenger.entity.enums.ParticipantConsentStatus;
import com.my.challenger.entity.quiz.QuizParticipantSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizParticipantSettingsRepository extends JpaRepository<QuizParticipantSettings, Long> {
    
    Optional<QuizParticipantSettings> findByChallengeIdAndUserId(Long challengeId, Long userId);
    
    List<QuizParticipantSettings> findByChallengeId(Long challengeId);
    
    @Query("SELECT qps FROM QuizParticipantSettings qps WHERE qps.challenge.id = :challengeId AND qps.resultConsentStatus = :status")
    List<QuizParticipantSettings> findByChallengeIdAndConsentStatus(Long challengeId, ParticipantConsentStatus status);
    
    @Query("SELECT COUNT(qps) FROM QuizParticipantSettings qps WHERE qps.challenge.id = :challengeId")
    long countParticipantsByChallenge(Long challengeId);
    
    @Query("SELECT qps FROM QuizParticipantSettings qps WHERE qps.challenge.id = :challengeId AND qps.resultConsentStatus = 'GRANTED'")
    List<QuizParticipantSettings> findConsentedParticipants(Long challengeId);
    
    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);
}
