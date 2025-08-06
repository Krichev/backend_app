package com.my.challenger.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.*;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.QuizRound;
import com.my.challenger.entity.quiz.QuizSession;
import com.my.challenger.repository.*;
import com.my.challenger.service.WWWGameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EnhancedQuizService extends QuizService {

    private final ObjectMapper objectMapper;
    private final QuizRoundRepository quizRoundRepository;

    public EnhancedQuizService(
            QuizQuestionRepository quizQuestionRepository,
            QuizSessionRepository quizSessionRepository,
            QuizRoundRepository quizRoundRepository,
            ChallengeRepository challengeRepository,
            UserRepository userRepository,
            WWWGameService gameService,
            ObjectMapper objectMapper) {

        super(quizQuestionRepository, quizSessionRepository, quizRoundRepository,
                challengeRepository, userRepository, gameService);

        this.objectMapper = objectMapper;
        this.quizRoundRepository = quizRoundRepository;
    }

    /**
     * Create a quiz challenge with automatic question saving
     */
    @Transactional
    public ChallengeDTO createQuizChallenge(CreateQuizChallengeRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

        // 1. Create the quiz challenge
        Challenge challenge = new Challenge();
        challenge.setType(ChallengeType.QUIZ);
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setCreator(creator);
        challenge.setPublic(request.getVisibility().equals("PUBLIC"));
        challenge.setVerificationMethod(VerificationMethod.QUIZ);
        challenge.setStartDate(request.getStartDate() != null ?
                request.getStartDate() : LocalDateTime.now());
        challenge.setEndDate(request.getEndDate());
        challenge.setFrequency(request.getFrequency());
        challenge.setStatus(ChallengeStatus.ACTIVE);

        // 2. Save quiz configuration
        if (request.getQuizConfig() != null) {
            try {
                String quizConfigJson = objectMapper.writeValueAsString(request.getQuizConfig());
                challenge.setQuizConfig(quizConfigJson);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid quiz configuration", e);
            }
        }

        Challenge savedChallenge = challengeRepository.save(challenge);

        // 3. Save custom questions if provided
        if (request.getCustomQuestions() != null && !request.getCustomQuestions().isEmpty()) {
            saveCustomQuestionsForChallenge(request.getCustomQuestions(), creator, savedChallenge.getId());
        }

        return convertChallengeToDTO(savedChallenge);
    }

    /**
     * Save custom questions for a challenge
     */
    @Transactional
    public List<QuizQuestionDTO> saveCustomQuestionsForChallenge(
            List<CreateQuizQuestionRequest> questionRequests, User creator, Long challengeId) {

        List<QuizQuestion> savedQuestions = questionRequests.stream()
                .map(request -> {
                    QuizQuestion question = QuizQuestion.builder()
                            .question(request.getQuestion())
                            .answer(request.getAnswer())
                            .difficulty(request.getDifficulty())
                            .topic(request.getTopic())
                            .source("USER_CREATED_FOR_CHALLENGE_" + challengeId)
                            .additionalInfo(request.getAdditionalInfo())
                            .isUserCreated(true)
                            .creator(creator)
                            .usageCount(0)
                            .build();

                    return quizQuestionRepository.save(question);
                })
                .collect(Collectors.toList());

        return savedQuestions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get quiz questions for a challenge (user questions + app questions if needed)
     */
    public List<QuizQuestionDTO> getQuestionsForChallenge(Long challengeId, QuizDifficulty difficulty, int count) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        // First, try to get user-created questions for this challenge
        List<QuizQuestion> userQuestions = quizQuestionRepository
                .findByCreatorIdAndSourceContaining(
                        challenge.getCreator().getId(),
                        "USER_CREATED_FOR_CHALLENGE_" + challengeId);

        List<QuizQuestionDTO> questions = userQuestions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());

        // If we don't have enough user questions, supplement with app questions
        if (questions.size() < count) {
            int remainingCount = count - questions.size();
            List<QuizQuestion> appQuestions = quizQuestionRepository
                    .findRandomQuestionsByDifficulty(difficulty.name(), remainingCount);

            List<QuizQuestionDTO> appQuestionDTOs = appQuestions.stream()
                    .map(this::convertQuestionToDTO)
                    .collect(Collectors.toList());

            questions.addAll(appQuestionDTOs);
        }

        return questions;
    }

    /**
     * Enhanced quiz session creation that automatically links to challenge
     */
    @Transactional
    public QuizSessionDTO createQuizSessionForChallenge(Long challengeId, Long hostUserId,
                                                        QuizSessionConfig sessionConfig) {

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        if (challenge.getType() != ChallengeType.QUIZ) {
            throw new IllegalArgumentException("Challenge is not a quiz type");
        }

        User hostUser = userRepository.findById(hostUserId)
                .orElseThrow(() -> new IllegalArgumentException("Host user not found"));

        // Create quiz session linked to the challenge
        QuizSession session = QuizSession.builder()
                .challenge(challenge)
                .hostUser(hostUser)
                .teamName(sessionConfig.getTeamName())
                .teamMembers(String.join(",", sessionConfig.getTeamMembers()))
                .difficulty(sessionConfig.getDifficulty())
                .roundTimeSeconds(sessionConfig.getRoundTimeSeconds())
                .totalRounds(sessionConfig.getTotalRounds())
                .enableAiHost(sessionConfig.getEnableAiHost())
                .questionSource(sessionConfig.getQuestionSource())
                .status(QuizSessionStatus.CREATED)
                .build();

        QuizSession savedSession = quizSessionRepository.save(session);

        // Get questions for this specific challenge
        List<QuizQuestionDTO> questions = getQuestionsForChallenge(
                challengeId, sessionConfig.getDifficulty(), sessionConfig.getTotalRounds());

        // Create quiz rounds
        createQuizRoundsFromQuestions(savedSession, questions);

        return convertSessionToDTO(savedSession);
    }

    /**
     * Create quiz rounds from a list of questions
     */
    private void createQuizRoundsFromQuestions(QuizSession session, List<QuizQuestionDTO> questions) {
        for (int i = 0; i < questions.size() && i < session.getTotalRounds(); i++) {
            QuizQuestionDTO questionDTO = questions.get(i);
            QuizQuestion question = quizQuestionRepository.findById(Long.valueOf(questionDTO.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Question not found"));

            QuizRound round = QuizRound.builder()
                    .quizSession(session)
                    .question(question)
                    .roundNumber(i + 1)
                    .isCorrect(false)
                    .hintUsed(false)
                    .voiceRecordingUsed(false)
                    .build();

            quizRoundRepository.save(round);
        }
    }

    /**
     * Get all quiz sessions for a challenge
     */
    public List<QuizSessionDTO> getQuizSessionsForChallenge(Long challengeId) {
        List<QuizSession> sessions = quizSessionRepository.findByChallengeIdOrderByCreatedAtDesc(challengeId);
        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get detailed session results including all rounds
     */
    public QuizSessionDetailDTO getQuizSessionDetail(Long sessionId, Long userId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found"));

        // Check if user has access (host or challenge participant)
        if (!session.getHostUser().getId().equals(userId) &&
                !isUserChallengeParticipant(session.getChallenge().getId(), userId)) {
            throw new IllegalStateException("Access denied to quiz session");
        }

        List<QuizRound> rounds = quizRoundRepository.findByQuizSessionIdOrderByRoundNumber(sessionId);

        List<QuizRoundDTO> roundDTOs = rounds.stream()
                .map(this::convertRoundToDTO)
                .collect(Collectors.toList());

        return QuizSessionDetailDTO.builder()
                .session(convertSessionToDTO(session))
                .rounds(roundDTOs)
                .totalScore(session.getCorrectAnswers())
                .accuracy(session.getTotalRounds() > 0 ?
                        (double) session.getCorrectAnswers() / session.getTotalRounds() * 100 : 0)
                .build();
    }

    /**
     * Update quiz session configuration
     */
    @Transactional
    public QuizSessionDTO updateQuizSessionConfig(Long sessionId, Long hostUserId,
                                                  QuizSessionConfig newConfig) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found"));

        if (!session.getHostUser().getId().equals(hostUserId)) {
            throw new IllegalStateException("You can only update your own quiz sessions");
        }

        if (session.getStatus() != QuizSessionStatus.CREATED) {
            throw new IllegalStateException("Cannot update session that has already started");
        }

        // Update session configuration
        session.setTeamName(newConfig.getTeamName());
        session.setTeamMembers(String.join(",", newConfig.getTeamMembers()));
        session.setDifficulty(newConfig.getDifficulty());
        session.setRoundTimeSeconds(newConfig.getRoundTimeSeconds());
        session.setTotalRounds(newConfig.getTotalRounds());
        session.setEnableAiHost(newConfig.getEnableAiHost());
        session.setQuestionSource(newConfig.getQuestionSource());

        QuizSession savedSession = quizSessionRepository.save(session);

        // If total rounds changed, recreate quiz rounds
        if (!session.getTotalRounds().equals(newConfig.getTotalRounds())) {
            // Delete existing rounds
            quizRoundRepository.deleteByQuizSessionId(sessionId);

            // Create new rounds with updated configuration
            List<QuizQuestionDTO> questions = getQuestionsForChallenge(
                    session.getChallenge().getId(),
                    newConfig.getDifficulty(),
                    newConfig.getTotalRounds());

            createQuizRoundsFromQuestions(savedSession, questions);
        }

        return convertSessionToDTO(savedSession);
    }

    /**
     * Archive completed quiz session
     */
    @Transactional
    public void archiveQuizSession(Long sessionId, Long hostUserId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found"));

        if (!session.getHostUser().getId().equals(hostUserId)) {
            throw new IllegalStateException("You can only archive your own quiz sessions");
        }

        if (session.getStatus() != QuizSessionStatus.COMPLETED) {
            throw new IllegalStateException("Can only archive completed sessions");
        }

        session.setStatus(QuizSessionStatus.ARCHIVED);
        quizSessionRepository.save(session);
    }

    /**
     * Get quiz statistics for a challenge
     */
    public QuizChallengeStatsDTO getQuizChallengeStats(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        if (challenge.getType() != ChallengeType.QUIZ) {
            throw new IllegalArgumentException("Challenge is not a quiz type");
        }

        List<QuizSession> sessions = quizSessionRepository.findByChallengeId(challengeId);

        int totalSessions = sessions.size();
        int completedSessions = (int) sessions.stream()
                .filter(s -> s.getStatus() == QuizSessionStatus.COMPLETED)
                .count();

        double avgScore = sessions.stream()
                .filter(s -> s.getStatus() == QuizSessionStatus.COMPLETED)
                .mapToInt(QuizSession::getCorrectAnswers)
                .average()
                .orElse(0.0);

        double avgAccuracy = sessions.stream()
                .filter(s -> s.getStatus() == QuizSessionStatus.COMPLETED && s.getTotalRounds() > 0)
                .mapToDouble(s -> (double) s.getCorrectAnswers() / s.getTotalRounds() * 100)
                .average()
                .orElse(0.0);

        return QuizChallengeStatsDTO.builder()
                .challengeId(challengeId)
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .averageScore(avgScore)
                .averageAccuracy(avgAccuracy)
                .totalQuestions(quizQuestionRepository.countByCreatorIdAndSourceContaining(
                        challenge.getCreator().getId(),
                        "USER_CREATED_FOR_CHALLENGE_" + challengeId))
                .build();
    }

    /**
     * Helper method to check if user is a participant in the challenge
     */
    private boolean isUserChallengeParticipant(Long challengeId, Long userId) {
        // This would typically check through a challenge participation table
        // For now, return true if challenge is public or user is creator
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        return challenge.isPublic() || challenge.getCreator().getId().equals(userId);
    }

    /**
     * Convert Challenge entity to DTO
     */
    private ChallengeDTO convertChallengeToDTO(Challenge challenge) {
        return ChallengeDTO.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .type(challenge.getType())
                .creator_id(challenge.getCreator().getId())
                .creatorUsername(challenge.getCreator().getUsername())
                .visibility(challenge.isPublic() ?
                        VisibilityType.PUBLIC : VisibilityType.PRIVATE)
                .verificationMethod(challenge.getVerificationMethod() != null ?
                        challenge.getVerificationMethod().toString() : null)
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .frequency(challenge.getFrequency())
                .status(challenge.getStatus())
                .quizConfig(challenge.getQuizConfig())
                .build();
    }

    /**
     * Convert QuizRound entity to DTO
     */
    private QuizRoundDTO convertRoundToDTO(QuizRound round) {
        return QuizRoundDTO.builder()
                .id(round.getId())
                .sessionId(round.getQuizSession().getId())
                .questionId(round.getQuestion().getId())
                .roundNumber(round.getRoundNumber())
                .question(round.getQuestion().getQuestion())
                .correctAnswer(round.getQuestion().getAnswer())
                .teamAnswer(round.getTeamAnswer())
                .playerWhoAnswered(round.getPlayerWhoAnswered())
                .isCorrect(round.getIsCorrect())
                .discussionNotes(round.getDiscussionNotes())
                .hintUsed(round.getHintUsed())
                .voiceRecordingUsed(round.getVoiceRecordingUsed())
                .aiFeedback(round.getAiFeedback())
                .answerSubmittedAt(round.getAnswerSubmittedAt())
                .build();
    }
}