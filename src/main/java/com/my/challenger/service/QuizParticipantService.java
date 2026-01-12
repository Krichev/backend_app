package com.my.challenger.service;

import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.quiz.QuizParticipantSettings;
import com.my.challenger.repository.ChallengeRepository;
import com.my.challenger.repository.QuizParticipantSettingsRepository;
import com.my.challenger.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizParticipantService {
    
    private final QuizParticipantSettingsRepository participantRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;

    /**
     * Join a quiz - creates participant settings entry
     */
    @Transactional
    public QuizParticipantSettings joinQuiz(Long challengeId, Long userId) {
        Challenge quiz = challengeRepository.findById(challengeId)
            .orElseThrow(() -> new EntityNotFoundException("Quiz not found"));
        
        // Check if already joined
        if (participantRepository.existsByChallengeIdAndUserId(challengeId, userId)) {
            return participantRepository.findByChallengeIdAndUserId(challengeId, userId).get();
        }
        
        // Check enrollment limits
        if (!quiz.canAcceptMoreParticipants()) {
            throw new IllegalStateException("Quiz has reached maximum participants");
        }
        
        if (!quiz.isEnrollmentOpen()) {
            throw new IllegalStateException("Enrollment deadline has passed");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        QuizParticipantSettings settings = QuizParticipantSettings.builder()
            .challenge(quiz)
            .user(user)
            .build();
        
        // Increment participant count
        quiz.setCurrentParticipantCount(quiz.getCurrentParticipantCount() + 1);
        challengeRepository.save(quiz);
        
        return participantRepository.save(settings);
    }

    /**
     * Check if user can start another attempt
     */
    public boolean canStartAttempt(Long challengeId, Long userId, Integer maxAttempts) {
        Optional<QuizParticipantSettings> settings = 
            participantRepository.findByChallengeIdAndUserId(challengeId, userId);
        
        if (settings.isEmpty()) {
            return true; // First attempt
        }
        
        int max = maxAttempts != null ? maxAttempts : 1;
        return settings.get().getAttemptsUsed() < max;
    }

    /**
     * Get results visible to creator (respecting consent)
     */
    @Transactional(readOnly = true)
    public List<QuizParticipantSettings> getResultsForCreator(Long challengeId, boolean requireConsent) {
        if (requireConsent) {
            return participantRepository.findConsentedParticipants(challengeId);
        }
        return participantRepository.findByChallengeId(challengeId);
    }

    /**
     * Update consent status
     */
    @Transactional
    public void updateConsent(Long challengeId, Long userId, boolean granted) {
        QuizParticipantSettings settings = participantRepository
            .findByChallengeIdAndUserId(challengeId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Participant not found"));
        
        if (granted) {
            settings.grantConsent();
        } else {
            settings.denyConsent();
        }
        
        participantRepository.save(settings);
    }
}
