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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizRoundRepository quizRoundRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final WWWGameService gameService;

    // =============================================================================
    // QUESTION MANAGEMENT METHODS
    // =============================================================================

    @Transactional
    public QuizQuestionDTO createUserQuestion(CreateQuizQuestionRequest request, Long creatorId) {
        log.info("Creating user question for creator: {}", creatorId);

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
        log.info("Created question with ID: {}", saved.getId());
        return convertQuestionToDTO(saved);
    }

    public List<QuizQuestionDTO> getUserQuestions(Long userId) {
        log.info("Getting user questions for user: {}", userId);

        List<QuizQuestion> questions = quizQuestionRepository
                .findByCreatorIdAndIsUserCreatedTrueOrderByCreatedAtDesc(userId);

        return questions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUserQuestion(Long questionId, Long userId) {
        log.info("Deleting question {} for user: {}", questionId, userId);

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
        log.info("Getting {} questions by difficulty: {}", count, difficulty);

        List<QuizQuestion> questions = quizQuestionRepository
                .findByDifficultyOrderByUsageCountAsc(difficulty, PageRequest.of(0, count));

        return questions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    public List<QuizQuestionDTO> searchQuestions(String keyword, int limit) {
        log.info("Searching questions with keyword: {} (limit: {})", keyword, limit);

        List<QuizQuestion> questions = quizQuestionRepository
                .searchByKeyword(keyword, PageRequest.of(0, limit));

        return questions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    // =============================================================================
    // QUIZ SESSION MANAGEMENT METHODS
    // =============================================================================

    @Transactional
    public QuizSessionDTO startQuizSession(StartQuizSessionRequest request, Long hostUserId) {
        log.info("Starting quiz session for host: {}", hostUserId);

        // Validate challenge exists
        Challenge challenge = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        User hostUser = userRepository.findById(hostUserId)
                .orElseThrow(() -> new IllegalArgumentException("Host user not found"));

        // Create quiz session
        QuizSession session = QuizSession.builder()
                .challenge(challenge)
                .creatorId(hostUserId)
                .hostUser(hostUser)
                .teamName(request.getTeamName())
                .teamMembers(String.join(",", request.getTeamMembers()))
                .difficulty(request.getDifficulty())
                .roundTimeSeconds(request.getRoundTimeSeconds())
                .totalRounds(request.getTotalRounds())
                .enableAiHost(request.getEnableAiHost())
                .questionSource(request.getQuestionSource()) // FIXED: Using correct property name
                .status(QuizSessionStatus.CREATED)
                .build();

        QuizSession savedSession = quizSessionRepository.save(session);

        // Create quiz rounds with questions
        createQuizRounds(savedSession, request);

        log.info("Created quiz session with ID: {}", savedSession.getId());
        return convertSessionToDTO(savedSession);
    }

    @Transactional
    public QuizSessionDTO beginQuizSession(Long sessionId, Long userId) {
        log.info("Beginning quiz session {} for user: {}", sessionId, userId);

        QuizSession session = findUserSession(sessionId, userId);

        if (session.getStatus() != QuizSessionStatus.CREATED) {
            throw new IllegalStateException("Session must be in CREATED status to begin");
        }

        session.setStatus(QuizSessionStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());

        QuizSession updated = quizSessionRepository.save(session);
        return convertSessionToDTO(updated);
    }

    @Transactional
    public QuizRoundDTO submitRoundAnswer(Long sessionId, SubmitRoundAnswerRequest request, Long userId) {
        log.info("Submitting answer for session {} round {} by user: {}",
                sessionId, request.getRoundNumber(), userId);

        QuizSession session = findUserSession(sessionId, userId);

        if (session.getStatus() != QuizSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session must be in progress to submit answers");
        }

        QuizRound round = quizRoundRepository
                .findByQuizSessionIdAndRoundNumber(sessionId, request.getRoundNumber())
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        if (round.getAnswerSubmittedAt() != null) {
            throw new IllegalStateException("Answer already submitted for this round");
        }

        // Validate answer
        boolean isCorrect = gameService.validateAnswer(
                request.getTeamAnswer(),
                round.getQuestion().getAnswer()
        );

        // Update round
        round.setTeamAnswer(request.getTeamAnswer());
        round.setIsCorrect(isCorrect);
        round.setPlayerWhoAnswered(request.getPlayerWhoAnswered());
        round.setDiscussionNotes(request.getDiscussionNotes());
        round.setHintUsed(request.getHintUsed() != null ? request.getHintUsed() : false);
        round.setVoiceRecordingUsed(request.getVoiceRecordingUsed() != null ? request.getVoiceRecordingUsed() : false);
        round.setAnswerSubmittedAt(LocalDateTime.now());

        // Generate AI feedback if enabled
        if (session.getEnableAiHost()) {
            String feedback = gameService.generateRoundFeedback(round, isCorrect);
            round.setAiFeedback(feedback);
        }

        QuizRound savedRound = quizRoundRepository.save(round);

        // Update session progress
        updateSessionProgress(session);

        return convertRoundToDTO(savedRound);
    }

    @Transactional
    public QuizSessionDTO completeQuizSession(Long sessionId, Long userId) {
        log.info("Completing quiz session {} for user: {}", sessionId, userId);

        QuizSession session = findUserSession(sessionId, userId);

        if (session.getStatus() == QuizSessionStatus.COMPLETED) {
            throw new IllegalStateException("Session is already completed");
        }

        session.setStatus(QuizSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());

        // Calculate total duration if started
        if (session.getStartedAt() != null) {
            long durationSeconds = java.time.Duration
                    .between(session.getStartedAt(), session.getCompletedAt())
                    .getSeconds();
            session.setTotalDurationSeconds((int) durationSeconds);
        }

        QuizSession updated = quizSessionRepository.save(session);
        return convertSessionToDTO(updated);
    }

    public QuizSessionDTO getQuizSession(Long sessionId, Long userId) {
        log.info("Getting quiz session {} for user: {}", sessionId, userId);

        QuizSession session = findUserSession(sessionId, userId);
        return convertSessionToDTO(session);
    }

    public List<QuizSessionDTO> getUserQuizSessions(Long userId, int limit) {
        log.info("Getting user quiz sessions for user: {} (limit: {})", userId, limit);

        // FIXED: Use correct repository method based on creatorId
        List<QuizSession> sessions = quizSessionRepository
                .findByCreatorIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));

        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }

    public List<QuizRoundDTO> getQuizRounds(Long sessionId, Long userId) {
        log.info("Getting quiz rounds for session {} by user: {}", sessionId, userId);

        // Verify user has access to this session
        findUserSession(sessionId, userId);

        List<QuizRound> rounds = quizRoundRepository
                .findByQuizSessionIdOrderByRoundNumber(sessionId);

        return rounds.stream()
                .map(this::convertRoundToDTO)
                .collect(Collectors.toList());
    }

    // =============================================================================
    // ENHANCED SEARCH METHODS (Fixed repository calls)
    // =============================================================================

    public List<QuizSessionDTO> getSessionsByQuestionSourceContaining(Long creatorId, String questionSource) {
        log.info("Getting sessions by creator {} with questionSource containing: {}", creatorId, questionSource);

        // FIXED: Now uses correct repository method name
        List<QuizSession> sessions = quizSessionRepository
                .findByCreatorIdAndQuestionSourceContaining(creatorId, questionSource);

        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }

    public List<QuizSessionDTO> getSessionsByExactQuestionSource(Long creatorId, String questionSource) {
        log.info("Getting sessions by creator {} with exact questionSource: {}", creatorId, questionSource);

        // FIXED: Now uses correct repository method name
        List<QuizSession> sessions = quizSessionRepository
                .findByCreatorIdAndQuestionSource(creatorId, questionSource);

        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }

    public List<QuizSessionDTO> getRecentSessions(Long creatorId, int daysBack) {
        log.info("Getting recent sessions for creator {} ({} days back)", creatorId, daysBack);

        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        List<QuizSession> sessions = quizSessionRepository
                .findRecentSessionsByCreator(creatorId, since);

        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }

    public List<QuizSessionDTO> searchSessions(QuizSessionSearchCriteria criteria) {
        log.info("Searching sessions with criteria: {}", criteria);

        PageRequest pageRequest = PageRequest.of(0, criteria.getLimit());
        List<QuizSession> sessions = quizSessionRepository.findSessionsWithCriteria(
                criteria.getCreatorId(),
                criteria.getQuestionSource(),
                criteria.getStatus(),
                pageRequest
        );

        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }

    public QuizSessionStatsDTO getSessionStats(Long creatorId) {
        log.info("Getting session stats for creator: {}", creatorId);

        long totalSessions = quizSessionRepository.countByCreatorId(creatorId);
        long completedSessions = quizSessionRepository.countByCreatorIdAndStatus(creatorId, QuizSessionStatus.COMPLETED);
        long activeSessions = quizSessionRepository.countByCreatorIdAndStatus(creatorId, QuizSessionStatus.IN_PROGRESS);

        return QuizSessionStatsDTO.builder()
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .activeSessions(activeSessions)
                .completionRate(totalSessions > 0 ? (double) completedSessions / totalSessions * 100 : 0)
                .build();
    }

    public QuizRoundDTO getCurrentRound(Long sessionId, Long userId) {
        log.info("Getting current round for session {} by user: {}", sessionId, userId);

        // Verify user has access to this session
        findUserSession(sessionId, userId);

        // Find the first round that hasn't been submitted yet
        List<QuizRound> rounds = quizRoundRepository
                .findByQuizSessionIdOrderByRoundNumber(sessionId);

        Optional<QuizRound> currentRound = rounds.stream()
                .filter(round -> round.getAnswerSubmittedAt() == null)
                .findFirst();

        if (currentRound.isEmpty()) {
            throw new IllegalStateException("No current round found - all rounds may be completed");
        }

        return convertRoundToDTO(currentRound.get());
    }

    @Transactional
    public QuizSessionDTO updateSessionConfig(Long sessionId, UpdateQuizSessionConfigRequest request, Long userId) {
        log.info("Updating session {} config for user: {}", sessionId, userId);

        QuizSession session = findUserSession(sessionId, userId);

        if (session.getStatus() != QuizSessionStatus.CREATED) {
            throw new IllegalStateException("Can only update configuration for sessions that haven't started");
        }

        // Update configurable fields
        if (request.getRoundTimeSeconds() != null) {
            session.setRoundTimeSeconds(request.getRoundTimeSeconds());
        }
        if (request.getEnableAiHost() != null) {
            session.setEnableAiHost(request.getEnableAiHost());
        }
        if (request.getTeamName() != null) {
            session.setTeamName(request.getTeamName());
        }
        if (request.getTeamMembers() != null) {
            session.setTeamMembers(request.getTeamMembers());
        }

        session.setUpdatedAt(LocalDateTime.now());
        QuizSession updated = quizSessionRepository.save(session);

        return convertSessionToDTO(updated);
    }

    @Transactional
    public void updateSessionStatus(Long sessionId, QuizSessionStatus status, Long userId) {
        log.info("Updating session {} status to {} for user: {}", sessionId, status, userId);

        QuizSession session = findUserSession(sessionId, userId);
        session.setStatus(status);
        session.setUpdatedAt(LocalDateTime.now());

        quizSessionRepository.save(session);
    }

    // =============================================================================
    // HELPER METHODS
    // =============================================================================

    private QuizSession findUserSession(Long sessionId, Long userId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getCreatorId().equals(userId)) {
            throw new IllegalStateException("You can only access your own sessions");
        }

        return session;
    }

    private void createQuizRounds(QuizSession session, StartQuizSessionRequest request) {
        log.info("Creating quiz rounds for session: {}", session.getId());

        List<QuizQuestion> questions;

        if ("user".equals(request.getQuestionSource()) && request.getCustomQuestionIds() != null) {
            // Use user-specified questions
            questions = quizQuestionRepository.findAllById(request.getCustomQuestionIds());
            if (questions.size() < request.getTotalRounds()) {
                throw new IllegalArgumentException("Not enough custom questions selected");
            }
        } else {
            // Use random questions by difficulty
            questions = quizQuestionRepository.findByDifficultyOrderByUsageCountAsc(
                    request.getDifficulty(), PageRequest.of(0, request.getTotalRounds()));
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
                    .hintUsed(false)
                    .voiceRecordingUsed(false)
                    .build();
            quizRoundRepository.save(round);
        }
    }

    private void updateSessionProgress(QuizSession session) {
        long completedRounds = quizRoundRepository
                .countByQuizSessionIdAndAnswerSubmittedAtIsNotNull(session.getId());
        long correctAnswers = quizRoundRepository
                .countByQuizSessionIdAndIsCorrectTrue(session.getId());

        session.setCompletedRounds((int) completedRounds);
        session.setCorrectAnswers((int) correctAnswers);

        // If all rounds completed, mark session as complete
        if (completedRounds >= session.getTotalRounds()) {
            session.setStatus(QuizSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
        }

        quizSessionRepository.save(session);
    }

    // =============================================================================
    // DTO CONVERSION METHODS
    // =============================================================================

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
                .scorePercentage(calculateScorePercentage(session))
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

    private Double calculateScorePercentage(QuizSession session) {
        if (session.getTotalRounds() == null || session.getTotalRounds() == 0) {
            return 0.0;
        }
        return (double) session.getCorrectAnswers() / session.getTotalRounds() * 100;
    }
}