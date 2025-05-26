// src/main/java/com/my/challenger/service/impl/QuizService.java
package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.QuizRound;
import com.my.challenger.entity.quiz.QuizSession;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuizSessionStatus;
import com.my.challenger.repository.*;
import com.my.challenger.service.WWWGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizRoundRepository quizRoundRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final WWWGameService gameService;

    // Question Management
    @Transactional
    public QuizQuestionDTO createUserQuestion(CreateQuizQuestionRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

        QuizQuestion question = QuizQuestion.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .difficulty(request.getDifficulty())
                .topic(request.getTopic())
                .source(request.getSource())
                .additionalInfo(request.getAdditionalInfo())
                .isUserCreated(true)
                .creator(creator)
                .usageCount(0)
                .build();

        QuizQuestion saved = quizQuestionRepository.save(question);
        return convertQuestionToDTO(saved);
    }

    public List<QuizQuestionDTO> getUserQuestions(Long userId) {
        List<QuizQuestion> questions = quizQuestionRepository.findByCreatorIdAndIsUserCreatedTrueOrderByCreatedAtDesc(userId);
        return questions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUserQuestion(Long questionId, Long userId) {
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        if (!question.getCreator().getId().equals(userId)) {
            throw new IllegalStateException("You can only delete your own questions");
        }

        if (!question.getIsUserCreated()) {
            throw new IllegalStateException("Cannot delete system questions");
        }

        quizQuestionRepository.delete(question);
    }

    public List<QuizQuestionDTO> getQuestionsByDifficulty(QuizDifficulty difficulty, int count) {
        List<QuizQuestion> questions = quizQuestionRepository.findRandomQuestionsByDifficulty(
                difficulty.name(), count);
        return questions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    public List<QuizQuestionDTO> searchQuestions(String keyword, int limit) {
        List<QuizQuestion> questions = quizQuestionRepository.searchByKeyword(
                keyword, PageRequest.of(0, limit));
        return questions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    // Quiz Session Management
    @Transactional
    public QuizSessionDTO startQuizSession(StartQuizSessionRequest request, Long hostUserId) {
        // Validate challenge exists
        Challenge challenge = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        User hostUser = userRepository.findById(hostUserId)
                .orElseThrow(() -> new IllegalArgumentException("Host user not found"));

        // Create quiz session
        QuizSession session = QuizSession.builder()
                .challenge(challenge)
                .hostUser(hostUser)
                .teamName(request.getTeamName())
                .teamMembers(String.join(",", request.getTeamMembers())) // Store as comma-separated string
                .difficulty(request.getDifficulty())
                .roundTimeSeconds(request.getRoundTimeSeconds())
                .totalRounds(request.getTotalRounds())
                .enableAiHost(request.getEnableAiHost())
                .questionSource(request.getQuestionSource())
                .status(QuizSessionStatus.CREATED)
                .build();

        QuizSession savedSession = quizSessionRepository.save(session);

        // Create quiz rounds with questions
        createQuizRounds(savedSession, request);

        return convertSessionToDTO(savedSession);
    }

    @Transactional
    public QuizSessionDTO beginQuizSession(Long sessionId, Long hostUserId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found"));

        if (!session.getHostUser().getId().equals(hostUserId)) {
            throw new IllegalStateException("You can only start your own quiz sessions");
        }

        if (session.getStatus() != QuizSessionStatus.CREATED) {
            throw new IllegalStateException("Quiz session is not in created state");
        }

        session.startSession();
        QuizSession savedSession = quizSessionRepository.save(session);

        return convertSessionToDTO(savedSession);
    }

    @Transactional
    public QuizRoundDTO submitRoundAnswer(Long sessionId, SubmitRoundAnswerRequest request, Long hostUserId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found"));

        if (!session.getHostUser().getId().equals(hostUserId)) {
            throw new IllegalStateException("You can only submit answers for your own quiz sessions");
        }

        QuizRound round = quizRoundRepository.findByQuizSessionIdAndRoundNumber(sessionId, request.getRoundNumber())
                .orElseThrow(() -> new IllegalArgumentException("Quiz round not found"));

        // Validate answer using game service
        boolean isCorrect = gameService.validateAnswer(request.getTeamAnswer(), round.getQuestion().getAnswer());

        // Update round with answer
        round.submitAnswer(request.getTeamAnswer(), request.getPlayerWhoAnswered());
        round.setIsCorrect(isCorrect);
        round.setDiscussionNotes(request.getDiscussionNotes());
        round.setHintUsed(request.getHintUsed());
        round.setVoiceRecordingUsed(request.getVoiceRecordingUsed());

        // Generate AI feedback if enabled
        if (session.getEnableAiHost()) {
            String feedback = gameService.generateRoundFeedback(round, isCorrect);
            round.setAiFeedback(feedback);
        }

        QuizRound savedRound = quizRoundRepository.save(round);

        // Update session statistics
        updateSessionProgress(session);

        // Update question usage count
        round.getQuestion().setUsageCount(round.getQuestion().getUsageCount() + 1);
        round.getQuestion().setLastUsed(LocalDateTime.now());
        quizQuestionRepository.save(round.getQuestion());

        return convertRoundToDTO(savedRound);
    }

    @Transactional
    public QuizSessionDTO completeQuizSession(Long sessionId, Long hostUserId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found"));

        if (!session.getHostUser().getId().equals(hostUserId)) {
            throw new IllegalStateException("You can only complete your own quiz sessions");
        }

        session.completeSession();
        QuizSession savedSession = quizSessionRepository.save(session);

        return convertSessionToDTO(savedSession);
    }

    public QuizSessionDTO getQuizSession(Long sessionId, Long userId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found"));

        if (!session.getHostUser().getId().equals(userId)) {
            throw new IllegalStateException("You can only view your own quiz sessions");
        }

        return convertSessionToDTO(session);
    }

    public List<QuizSessionDTO> getUserQuizSessions(Long userId, int limit) {
        List<QuizSession> sessions = quizSessionRepository.findByHostUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, limit));
        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }

    public List<QuizRoundDTO> getQuizRounds(Long sessionId, Long userId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found"));

        if (!session.getHostUser().getId().equals(userId)) {
            throw new IllegalStateException("You can only view rounds for your own quiz sessions");
        }

        List<QuizRound> rounds = quizRoundRepository.findByQuizSessionIdOrderByRoundNumber(sessionId);
        return rounds.stream()
                .map(this::convertRoundToDTO)
                .collect(Collectors.toList());
    }

    // Helper methods
    private void createQuizRounds(QuizSession session, StartQuizSessionRequest request) {
        List<QuizQuestion> questions;

        if ("user".equals(request.getQuestionSource()) && request.getCustomQuestionIds() != null) {
            // Use user-specified questions
            questions = quizQuestionRepository.findAllById(request.getCustomQuestionIds());
            if (questions.size() < request.getTotalRounds()) {
                throw new IllegalArgumentException("Not enough custom questions selected");
            }
        } else {
            // Use random questions by difficulty
            questions = quizQuestionRepository.findRandomQuestionsByDifficulty(
                    request.getDifficulty().name(), request.getTotalRounds());
            if (questions.size() < request.getTotalRounds()) {
                throw new IllegalStateException("Not enough questions available for the selected difficulty");
            }
        }

        // Create rounds
        for (int i = 0; i < request.getTotalRounds(); i++) {
            QuizRound round = QuizRound.builder()
                    .quizSession(session)
                    .question(questions.get(i))
                    .roundNumber(i + 1)
                    .build();
            quizRoundRepository.save(round);
        }
    }

    private void updateSessionProgress(QuizSession session) {
        long completedRounds = quizRoundRepository.countByQuizSessionIdAndAnswerSubmittedAtIsNotNull(session.getId());
        long correctAnswers = quizRoundRepository.countByQuizSessionIdAndIsCorrectTrue(session.getId());

        session.setCompletedRounds((int) completedRounds);
        session.setCorrectAnswers((int) correctAnswers);

        // If all rounds completed, mark session as complete
        if (completedRounds >= session.getTotalRounds()) {
            session.completeSession();
        }

        quizSessionRepository.save(session);
    }

    // DTO Conversion methods
    private QuizQuestionDTO convertQuestionToDTO(QuizQuestion question) {
        return QuizQuestionDTO.builder()
                .id(question.getId())
                .question(question.getQuestion())
                .answer(question.getAnswer())
                .difficulty(question.getDifficulty())
                .topic(question.getTopic())
                .source(question.getSource())
                .additionalInfo(question.getAdditionalInfo())
                .isUserCreated(question.getIsUserCreated())
                .creatorId(question.getCreator() != null ? question.getCreator().getId() : null)
                .externalId(question.getExternalId())
                .usageCount(question.getUsageCount())
                .createdAt(question.getCreatedAt())
                .lastUsed(question.getLastUsed())
                .build();
    }

    private QuizSessionDTO convertSessionToDTO(QuizSession session) {
        List<String> teamMembers = session.getTeamMembers() != null ? 
                List.of(session.getTeamMembers().split(",")) : List.of();

        return QuizSessionDTO.builder()
                .id(session.getId())
                .challengeId(session.getChallenge().getId())
                .challengeTitle(session.getChallenge().getTitle())
                .hostUserId(session.getHostUser().getId())
                .hostUsername(session.getHostUser().getUsername())
                .teamName(session.getTeamName())
                .teamMembers(teamMembers)
                .difficulty(session.getDifficulty())
                .roundTimeSeconds(session.getRoundTimeSeconds())
                .totalRounds(session.getTotalRounds())
                .completedRounds(session.getCompletedRounds())
                .correctAnswers(session.getCorrectAnswers())
                .scorePercentage(session.getScorePercentage())
                .enableAiHost(session.getEnableAiHost())
                .questionSource(session.getQuestionSource())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .totalDurationSeconds(session.getTotalDurationSeconds())
                .createdAt(session.getCreatedAt())
                .build();
    }

    private QuizRoundDTO convertRoundToDTO(QuizRound round) {
        return QuizRoundDTO.builder()
                .id(round.getId())
                .quizSessionId(round.getQuizSession().getId())
                .question(convertQuestionToDTO(round.getQuestion()))
                .roundNumber(round.getRoundNumber())
                .teamAnswer(round.getTeamAnswer())
                .isCorrect(round.getIsCorrect())
                .playerWhoAnswered(round.getPlayerWhoAnswered())
                .discussionNotes(round.getDiscussionNotes())
                .roundStartedAt(round.getRoundStartedAt())
                .answerSubmittedAt(round.getAnswerSubmittedAt())
                .discussionDurationSeconds(round.getDiscussionDurationSeconds())
                .totalRoundDurationSeconds(round.getTotalRoundDurationSeconds())
                .hintUsed(round.getHintUsed())
                .voiceRecordingUsed(round.getVoiceRecordingUsed())
                .aiFeedback(round.getAiFeedback())
                .build();
    }
}