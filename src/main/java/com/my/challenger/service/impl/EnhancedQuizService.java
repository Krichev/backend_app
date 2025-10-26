package com.my.challenger.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.Task;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.*;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.QuizRound;
import com.my.challenger.entity.quiz.QuizSession;
import com.my.challenger.entity.quiz.Topic;
import com.my.challenger.mapper.QuizQuestionMapper;
import com.my.challenger.repository.*;
import com.my.challenger.service.WWWGameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EnhancedQuizService extends QuizService {

//    private final ObjectMapper objectMapper;
    private final QuizRoundRepository quizRoundRepository;
    private final TaskRepository taskRepository;
    private final TopicService topicService;

    public EnhancedQuizService(
            QuizQuestionRepository quizQuestionRepository,
            QuizSessionRepository quizSessionRepository,
            QuizRoundRepository quizRoundRepository,
            ChallengeRepository challengeRepository,
            UserRepository userRepository,
            WWWGameService gameService,
            MinioMediaStorageService mediaStorageService,
            MediaFileRepository mediaFileRepository,
            ObjectMapper objectMapper,
            TaskRepository taskRepository, TopicService topicService) {

        super(quizQuestionRepository, quizSessionRepository, quizRoundRepository,
                challengeRepository, userRepository, mediaFileRepository, gameService,
                mediaStorageService, topicService);

//        this.objectMapper = objectMapper;
        this.quizRoundRepository = quizRoundRepository;
        this.taskRepository = taskRepository;
        this.topicService = topicService;
    }


    /**
     * Map quiz difficulty string to ChallengeDifficulty enum
     */
    private ChallengeDifficulty mapQuizDifficultyToChallengeDifficulty(String quizDifficulty) {
        if (quizDifficulty == null) {
            return ChallengeDifficulty.MEDIUM;
        }

        return switch (quizDifficulty.toUpperCase()) {
            case "EASY" -> ChallengeDifficulty.EASY;
            case "MEDIUM" -> ChallengeDifficulty.MEDIUM;
            case "HARD" -> ChallengeDifficulty.HARD;
            default -> {
                log.warn("Unknown quiz difficulty: {}, defaulting to MEDIUM", quizDifficulty);
                yield ChallengeDifficulty.MEDIUM;
            }
        };
    }


    /**
     * Create a new quiz challenge with complete configuration
     *
     * @param request   The quiz challenge creation request
     * @param creatorId The ID of the user creating the challenge
     * @return The created challenge DTO with saved configuration
     */
    @Transactional
    public ChallengeDTO createQuizChallenge(CreateQuizChallengeRequest request, Long creatorId) {
        try {
            log.info("=== Starting Quiz Challenge Creation ===");
            log.info("Creator ID: {}", creatorId);
            log.info("Challenge Title: {}", request.getTitle());

            // 1. Validate and fetch creator
            User creator = userRepository.findById(creatorId)
                    .orElseThrow(() -> new IllegalArgumentException("Creator not found with ID: " + creatorId));
            log.info("Creator found: {}", creator.getUsername());

            // 2. Create the Challenge entity
            Challenge challenge = new Challenge();
            challenge.setType(ChallengeType.QUIZ);
            challenge.setTitle(request.getTitle());
            challenge.setDescription(request.getDescription());
            challenge.setCreator(creator);
            challenge.setPublic(request.getVisibility().equals("PUBLIC"));

            // Set verification method - CRITICAL for quiz challenges
            challenge.setVerificationMethod(VerificationMethod.QUIZ);
            log.info("Verification method set to: QUIZ");

            // Set dates
            challenge.setStartDate(request.getStartDate() != null ?
                    request.getStartDate() : LocalDateTime.now());
            challenge.setEndDate(request.getEndDate());

            // Set frequency
            challenge.setFrequency(request.getFrequency() != null ?
                    request.getFrequency() : FrequencyType.ONE_TIME);

            // Set status
            challenge.setStatus(ChallengeStatus.ACTIVE);

            // 3. Save quiz configuration as JSON string
            if (request.getQuizConfig() != null) {
                QuizChallengeConfig config = request.getQuizConfig();

                // Log all configuration fields before saving
                log.info("=== Quiz Configuration Details ===");
                log.info("Game Type: {}", config.getGameType());
                log.info("Team Name: {}", config.getTeamName());
                log.info("Team Members: {}", config.getTeamMembers());
                log.info("Team Members Count: {}", config.getTeamMembers() != null ? config.getTeamMembers().size() : 0);
                log.info("Difficulty: {}", config.getDefaultDifficulty());
                log.info("Round Time (seconds): {}", config.getDefaultRoundTimeSeconds());
                log.info("Total Rounds: {}", config.getDefaultTotalRounds());
                log.info("AI Host Enabled: {}", config.getEnableAiHost());
                log.info("Question Source: {}", config.getQuestionSource());
                log.info("Allow Custom Questions: {}", config.getAllowCustomQuestions());
                log.info("Team Based: {}", config.getTeamBased());
                log.info("================================");

//                try {
//                    // Serialize the entire config object to JSON
//                    String quizConfigJson = objectMapper.writeValueAsString(config);
//                    challenge.setQuizConfig(quizConfigJson);
//
//                    log.info("Quiz config serialized successfully");
//                    log.debug("Serialized JSON: {}", quizConfigJson);
//
//                } catch (JsonProcessingException e) {
//                    log.error("Failed to serialize quiz configuration", e);
//                    throw new RuntimeException("Invalid quiz configuration: " + e.getMessage(), e);
//                }
            } else {
                log.warn("No quiz configuration provided!");
                throw new IllegalArgumentException("Quiz configuration is required for quiz challenges");
            }

            // 4. Save the challenge first to generate ID
            Challenge savedChallenge = challengeRepository.save(challenge);
            log.info("Challenge saved successfully with ID: {}", savedChallenge.getId());
            log.info("Verification method confirmed: {}", savedChallenge.getVerificationMethod());

            // 5. Save custom questions if provided
            if (request.getCustomQuestions() != null && !request.getCustomQuestions().isEmpty()) {
                log.info("Saving {} custom questions", request.getCustomQuestions().size());
                saveCustomQuestionsForChallenge(
                        request.getCustomQuestions(),
                        creator,
                        savedChallenge.getId()
                );
            } else {
                log.info("No custom questions provided");
            }

            // 6. Create initial task for the challenge
            createQuizTask(savedChallenge, creator);

            // 7. Convert to DTO and return
            ChallengeDTO challengeDTO = convertChallengeToDTO(savedChallenge);

            log.info("=== Quiz Challenge Created Successfully ===");
            log.info("Challenge ID: {}", challengeDTO.getId());
            log.info("Quiz Config in DTO: {}", challengeDTO.getQuizConfig());

            return challengeDTO;

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating quiz challenge", e);
            throw new RuntimeException("Failed to create quiz challenge: " + e.getMessage(), e);
        }
    }

    /**
     * Save questions to an existing challenge
     */
    @Transactional
    public void saveQuestionsToChallenge(Long challengeId, List<CreateQuizQuestionRequest> questions, Long creatorId) {
        try {
            log.info("Saving {} questions to challenge {} by user {}", questions.size(), challengeId, creatorId);

            // Validate challenge exists and user has permission
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

            if (!challenge.getCreator().getId().equals(creatorId)) {
                throw new IllegalArgumentException("Only challenge creator can add questions");
            }

            User creator = userRepository.findById(creatorId)
                    .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

            // Save the questions
            saveCustomQuestionsForChallenge(questions, creator, challenge.getId());

            log.info("Successfully saved {} questions to challenge {}", questions.size(), challengeId);

        } catch (Exception e) {
            log.error("Error saving questions to challenge {}: {}", challengeId, e.getMessage(), e);
            throw new RuntimeException("Failed to save questions to challenge: " + e.getMessage(), e);
        }
    }

    /**
     * Create a task for the quiz challenge
     */
    private void createQuizTask(Challenge challenge, User creator) {
        log.info("Creating task for challenge ID: {}", challenge.getId());

        try {
            Task task = new Task();
            task.setChallenge(challenge);
            task.setAssignedToUser(creator);
            task.setTitle("Complete Quiz: " + challenge.getTitle());
            task.setDescription("Participate in the quiz challenge and answer all questions");
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());

            Task savedTask = taskRepository.save(task);
            log.info("Task created successfully with ID: {}", savedTask.getId());

        } catch (Exception e) {
            log.error("Error creating task for challenge", e);
            // Don't fail the entire challenge creation if task creation fails
        }
    }


    /**
     * Save custom questions for a challenge
     */
    public List<QuizQuestionDTO> saveCustomQuestionsForChallenge(
            List<CreateQuizQuestionRequest> questionRequests,
            User creator,
            Long challengeId) {



        log.info("Saving custom questions for challenge ID: {}", challengeId);
        List<QuizQuestionDTO> questions = new ArrayList<>();
        for (CreateQuizQuestionRequest questionRequest : questionRequests) {
            try {

                Topic topic = null;
                if (questionRequest.getTopic() != null && !questionRequest.getTopic().isBlank()) {
                    topic = topicService.getOrCreateTopic(questionRequest.getTopic());
                }
                QuizQuestion question = QuizQuestion.builder()
                        .question(questionRequest.getQuestion())
                        .answer(questionRequest.getAnswer())
                        .difficulty(questionRequest.getDifficulty() != null ?
                                questionRequest.getDifficulty() : QuizDifficulty.MEDIUM)
                        .topic(topic)
                        .additionalInfo(questionRequest.getAdditionalInfo())
                        .isUserCreated(true)
                        .creator(creator)
                        .usageCount(0)
                        .source("USER_CREATED_FOR_CHALLENGE_" + challengeId)
                        .questionType(QuestionType.TEXT)
                        .build();

                QuizQuestion savedQuestion = quizQuestionRepository.save(question);
                log.debug("Saved question ID: {} - {}", savedQuestion.getId(),
                        savedQuestion.getQuestion().substring(0, Math.min(50, savedQuestion.getQuestion().length())));

                questions.add(QuizQuestionMapper.INSTANCE.toDTO(savedQuestion));
            } catch (Exception e) {
                log.error("Error saving question: {}", questionRequest.getQuestion(), e);
                // Continue with other questions even if one fails
            }
        }

        log.info("Finished saving custom questions");
        return questions;
    }


    /**
     * Get quiz questions for a challenge (user questions + app questions if needed)
     */
    public List<QuizQuestionDTO> getQuestionsForChallenge(Long challengeId, QuizDifficulty difficulty, int count) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        // First, try to get user-created questions for this challenge
        List<QuizQuestion> userQuestions = quizQuestionRepository
                .findByCreator_IdAndSourceContaining(
                        challenge.getCreator().getId(),
                        "USER_CREATED_FOR_CHALLENGE_" + challengeId);

        List<QuizQuestionDTO> questions = userQuestions.stream()
                .map(QuizQuestionMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());

        // If we don't have enough user questions, supplement with app questions
        if (questions.size() < count) {
            int remainingCount = count - questions.size();
            List<QuizQuestion> appQuestions = quizQuestionRepository
                    .findByDifficulty(difficulty, PageRequest.of(0, remainingCount));

            List<QuizQuestionDTO> appQuestionDTOs = appQuestions.stream()
                    .map(QuizQuestionMapper.INSTANCE::toDTO)
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
     * Archive completed quiz session - FINAL VERSION
     */
    @Transactional
    public void archiveQuizSession(Long sessionId, Long hostUserId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found"));

        if (!session.getHostUser().getId().equals(hostUserId)) {
            throw new IllegalStateException("You can only archive your own quiz sessions");
        }

        // Use the helper method on the entity (if you added it)
        session.archiveSession();
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
                .totalQuestions(quizQuestionRepository.countByCreator_IdAndSourceContaining(
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
                .visibility(challenge.isPublic() ?
                        VisibilityType.PUBLIC : VisibilityType.PRIVATE)
                .status(challenge.getStatus())
                .created_at(LocalDateTime.now())
                .updated_at(LocalDateTime.now())
                .creator_id(challenge.getCreator().getId())
                .creatorUsername(challenge.getCreator().getUsername())
                .verificationMethod(challenge.getVerificationMethod() != null ?
                        challenge.getVerificationMethod().toString() : null)
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .frequency(challenge.getFrequency())
//                .quizConfig(challenge.getQuizConfig()) // This contains the full JSON with all fields
                .userIsCreator(true) // Set to true since we're the creator
                .build();
    }

    /**
     * Convert QuizRound entity to DTO
     */
    private QuizRoundDTO convertRoundToDTO(QuizRound round) {
        return QuizRoundDTO.builder()
                .id(round.getId())
                .quizSessionId(round.getQuizSession().getId())  // FIXED: was .sessionId()
                .question(QuizQuestionMapper.INSTANCE.toDTO(round.getQuestion()))  // FIXED: was .questionId() and .question()
                .roundNumber(round.getRoundNumber())
                .teamAnswer(round.getTeamAnswer())
                .isCorrect(round.getIsCorrect())
                .playerWhoAnswered(round.getPlayerWhoAnswered())
                .discussionNotes(round.getDiscussionNotes())
                .roundStartedAt(round.getRoundStartedAt())  // FIXED: was missing
                .answerSubmittedAt(round.getAnswerSubmittedAt())
                .discussionDurationSeconds(round.getDiscussionDurationSeconds())  // FIXED: was missing
                .totalRoundDurationSeconds(round.getTotalRoundDurationSeconds())  // FIXED: was missing
                .hintUsed(round.getHintUsed())
                .voiceRecordingUsed(round.getVoiceRecordingUsed())
                .aiFeedback(round.getAiFeedback())
                .build();
    }
}