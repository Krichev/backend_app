package com.my.challenger.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.dto.quiz.BrainRingAnswerResponse;
import com.my.challenger.dto.quiz.BrainRingStateDTO;
import com.my.challenger.dto.quiz.BuzzResponse;
import com.my.challenger.dto.quiz.AnswerValidationResult;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.BrainRingRoundStatus;
import com.my.challenger.entity.enums.QuizSessionStatus;
import com.my.challenger.entity.quiz.BrainRingRoundState;
import com.my.challenger.entity.quiz.QuizRound;
import com.my.challenger.entity.quiz.QuizSession;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.BrainRingRoundStateRepository;
import com.my.challenger.repository.QuizRoundRepository;
import com.my.challenger.repository.QuizSessionRepository;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.BrainRingService;
import com.my.challenger.service.WWWGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrainRingServiceImpl implements BrainRingService {

    private final BrainRingRoundStateRepository brainRingRoundStateRepository;
    private final QuizRoundRepository quizRoundRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository;
    private final WWWGameService gameService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BuzzResponse processBuzz(Long sessionId, Long roundId, Long userId, Instant clientTimestamp) {
        log.info("Processing buzz for session {}, round {}, user {}", sessionId, roundId, userId);

        BrainRingRoundState state = brainRingRoundStateRepository.findByQuizRoundId(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Brain Ring state not found for round " + roundId));

        if (state.getRoundStatus() != BrainRingRoundStatus.WAITING_FOR_BUZZ) {
            return BuzzResponse.builder()
                    .success(false)
                    .isFirstBuzzer(false)
                    .message("Round is not in waiting for buzz status")
                    .build();
        }

        List<Long> lockedOut = getLockedOutPlayers(state);
        if (lockedOut.contains(userId)) {
            return BuzzResponse.builder()
                    .success(false)
                    .isFirstBuzzer(false)
                    .message("Player is locked out for this round")
                    .build();
        }

        // Record buzz in order
        List<Map<String, Object>> buzzOrder = getBuzzOrder(state);
        buzzOrder.add(Map.of("userId", userId, "timestamp", clientTimestamp.toString()));
        state.setBuzzOrder(toJson(buzzOrder));

        // If no current buzzer, this user becomes the buzzer
        if (state.getCurrentBuzzer() == null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            
            state.setCurrentBuzzer(user);
            state.setBuzzerTimestamp(clientTimestamp);
            state.setRoundStatus(BrainRingRoundStatus.PLAYER_ANSWERING);
            
            QuizSession session = state.getQuizRound().getQuizSession();
            int answerTime = session.getAnswerTimeSeconds() != null ? session.getAnswerTimeSeconds() : 20;
            state.setAnswerDeadline(Instant.now().plusSeconds(answerTime));

            brainRingRoundStateRepository.save(state);

            return BuzzResponse.builder()
                    .success(true)
                    .isFirstBuzzer(true)
                    .answerDeadline(state.getAnswerDeadline())
                    .message("You are the buzzer!")
                    .build();
        }

        brainRingRoundStateRepository.save(state);
        return BuzzResponse.builder()
                .success(true)
                .isFirstBuzzer(false)
                .message("Recorded, but someone else was faster")
                .build();
    }

    @Override
    @Transactional
    public BrainRingAnswerResponse submitAnswer(Long sessionId, Long roundId, Long userId, String answer) {
        log.info("Submitting Brain Ring answer for session {}, round {}, user {}", sessionId, roundId, userId);

        BrainRingRoundState state = brainRingRoundStateRepository.findByQuizRoundId(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Brain Ring state not found for round " + roundId));

        if (state.getRoundStatus() != BrainRingRoundStatus.PLAYER_ANSWERING) {
            throw new IllegalStateException("Round is not in player answering status");
        }

        if (state.getCurrentBuzzer() == null || !state.getCurrentBuzzer().getId().equals(userId)) {
            throw new IllegalStateException("Only the current buzzer can submit an answer");
        }

        if (Instant.now().isAfter(state.getAnswerDeadline())) {
            // Timeout - treat as wrong answer
            return handleWrongAnswer(state, userId, "Time expired");
        }

        QuizRound round = state.getQuizRound();
        boolean enableAiValidation = round.getQuizSession().getEnableAiAnswerValidation() != null 
                && round.getQuizSession().getEnableAiAnswerValidation();
        
        AnswerValidationResult validationResult = gameService.validateAnswerEnhanced(
                answer,
                round.getQuestion().getAnswer(),
                enableAiValidation,
                "en"
        );

        if (validationResult.isCorrect()) {
            return handleCorrectAnswer(state, userId, answer, validationResult);
        } else {
            return handleWrongAnswer(state, userId, answer);
        }
    }

    private BrainRingAnswerResponse handleCorrectAnswer(BrainRingRoundState state, Long userId, String answer, AnswerValidationResult validationResult) {
        state.setRoundStatus(BrainRingRoundStatus.CORRECT_ANSWER);
        state.setWinner(state.getCurrentBuzzer());
        
        QuizRound round = state.getQuizRound();
        round.setTeamAnswer(answer);
        round.setIsCorrect(true);
        round.setPlayerWhoAnswered(state.getCurrentBuzzer().getUsername());
        round.setAnswerSubmittedAt(LocalDateTime.now());
        round.setAiValidationUsed(validationResult.isAiUsed());
        round.setAiAccepted(validationResult.isAiAccepted());
        
        quizRoundRepository.save(round);
        updateSessionProgress(round.getQuizSession());
        brainRingRoundStateRepository.save(state);

        return BrainRingAnswerResponse.builder()
                .isCorrect(true)
                .playerLockedOut(false)
                .roundComplete(true)
                .winnerUserId(userId)
                .build();
    }

    private BrainRingAnswerResponse handleWrongAnswer(BrainRingRoundState state, Long userId, String answer) {
        List<Long> lockedOut = getLockedOutPlayers(state);
        lockedOut.add(userId);
        state.setLockedOutPlayers(toJson(lockedOut));
        
        state.setCurrentBuzzer(null);
        state.setBuzzerTimestamp(null);
        state.setAnswerDeadline(null);

        // Check if all players are locked out
        // For simplicity, we assume we know the total players or just allow more buzzes
        // A better implementation would check against session participant count
        
        state.setRoundStatus(BrainRingRoundStatus.WAITING_FOR_BUZZ);
        brainRingRoundStateRepository.save(state);

        return BrainRingAnswerResponse.builder()
                .isCorrect(false)
                .playerLockedOut(true)
                .roundComplete(false)
                .nextBuzzerAllowed(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BrainRingStateDTO getRoundState(Long sessionId, Long roundId) {
        BrainRingRoundState state = brainRingRoundStateRepository.findByQuizRoundId(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Brain Ring state not found for round " + roundId));

        return BrainRingStateDTO.builder()
                .currentBuzzerUserId(state.getCurrentBuzzer() != null ? state.getCurrentBuzzer().getId() : null)
                .currentBuzzerName(state.getCurrentBuzzer() != null ? state.getCurrentBuzzer().getUsername() : null)
                .lockedOutPlayers(getLockedOutPlayers(state))
                .answerDeadline(state.getAnswerDeadline())
                .roundStatus(state.getRoundStatus())
                .winnerUserId(state.getWinner() != null ? state.getWinner().getId() : null)
                .build();
    }

    @Override
    @Transactional
    public void initializeRoundState(QuizRound round) {
        BrainRingRoundState state = BrainRingRoundState.builder()
                .quizRound(round)
                .roundStatus(BrainRingRoundStatus.WAITING_FOR_BUZZ)
                .lockedOutPlayers("[]")
                .buzzOrder("[]")
                .build();
        brainRingRoundStateRepository.save(state);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPlayerLockedOut(Long roundId, Long userId) {
        Optional<BrainRingRoundState> state = brainRingRoundStateRepository.findByQuizRoundId(roundId);
        return state.map(s -> getLockedOutPlayers(s).contains(userId)).orElse(false);
    }

    private List<Long> getLockedOutPlayers(BrainRingRoundState state) {
        try {
            if (state.getLockedOutPlayers() == null || state.getLockedOutPlayers().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(state.getLockedOutPlayers(), new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing locked out players JSON", e);
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getBuzzOrder(BrainRingRoundState state) {
        try {
            if (state.getBuzzOrder() == null || state.getBuzzOrder().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(state.getBuzzOrder(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing buzz order JSON", e);
            return new ArrayList<>();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting to JSON", e);
            return "[]";
        }
    }

    private void updateSessionProgress(QuizSession session) {
        long completedRounds = quizRoundRepository
                .countByQuizSessionIdAndAnswerSubmittedAtIsNotNull(session.getId());
        long correctAnswers = quizRoundRepository
                .countByQuizSessionIdAndIsCorrectTrue(session.getId());

        session.setCompletedRounds((int) completedRounds);
        session.setCorrectAnswers((int) correctAnswers);

        if (completedRounds >= session.getTotalRounds()) {
            session.setStatus(QuizSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
        }

        quizSessionRepository.save(session);
    }
}
