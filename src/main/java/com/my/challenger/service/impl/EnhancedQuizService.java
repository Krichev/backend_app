package com.my.challenger.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.Task;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.*;
import com.my.challenger.entity.enums.TaskType;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.QuizRound;
import com.my.challenger.entity.quiz.QuizSession;
import com.my.challenger.entity.quiz.Topic;
import com.my.challenger.entity.quiz.ChallengeQuestionAssignment;
import com.my.challenger.mapper.QuizQuestionMapper;
import com.my.challenger.repository.*;
import com.my.challenger.service.BrainRingService;
import com.my.challenger.service.WWWGameService;
import com.my.challenger.service.WagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.my.challenger.entity.ChallengeProgress;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.my.challenger.dto.QuizSessionSummaryDTO;
import com.my.challenger.dto.request.ReplayChallengeRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EnhancedQuizService extends QuizService {

    private final ObjectMapper objectMapper;
    private final TaskRepository taskRepository;
    private final ChallengeProgressRepository challengeProgressRepository;
    private final ChallengeQuestionAssignmentRepository challengeQuestionAssignmentRepository;

    public EnhancedQuizService(
            QuizQuestionRepository quizQuestionRepository,
            QuizSessionRepository quizSessionRepository,
            QuizRoundRepository quizRoundRepository,
            ChallengeRepository challengeRepository,
            UserRepository userRepository,
            MediaFileRepository mediaFileRepository,
            QuestRepository questRepository,
            WWWGameService gameService,
            MinioMediaStorageService mediaStorageService,
            ObjectMapper objectMapper,
            TaskRepository taskRepository,
            TopicService topicService,
            ChallengeProgressRepository challengeProgressRepository,
            QuizQuestionDTOEnricher dtoEnricher,
            WagerService wagerService,
            BrainRingService brainRingService,
            ChallengeQuestionAssignmentRepository challengeQuestionAssignmentRepository) {

        super(quizQuestionRepository, quizSessionRepository, quizRoundRepository,
                challengeRepository, userRepository, mediaFileRepository, questRepository, gameService,
                mediaStorageService, topicService, dtoEnricher, wagerService, brainRingService);

        this.objectMapper = objectMapper;
        this.taskRepository = taskRepository;
        this.challengeProgressRepository = challengeProgressRepository;
        this.challengeQuestionAssignmentRepository = challengeQuestionAssignmentRepository;
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

            // Map and set difficulty from quiz config
            if (request.getQuizConfig() != null && request.getQuizConfig().getDefaultDifficulty() != null) {
                challenge.setDifficulty(mapQuizDifficultyToChallengeDifficulty(
                        request.getQuizConfig().getDefaultDifficulty().name()));
                log.info("Difficulty set to: {}", challenge.getDifficulty());
            } else {
                challenge.setDifficulty(ChallengeDifficulty.MEDIUM);
                log.warn("No difficulty in quiz config, defaulting to MEDIUM");
            }

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

                try {
                    // Serialize the entire config object to JSON
                    String quizConfigJson = objectMapper.writeValueAsString(config);
                    challenge.setQuizConfig(quizConfigJson);

                    log.info("Quiz config serialized successfully");
                    log.debug("Serialized JSON: {}", quizConfigJson);

                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize quiz configuration", e);
                    throw new RuntimeException("Invalid quiz configuration: " + e.getMessage(), e);
                }
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

            // Step 5b: Save selected existing question assignments
            if (request.getSelectedQuestionIds() != null && !request.getSelectedQuestionIds().isEmpty()) {
                log.info("Assigning {} existing questions to challenge {}",
                        request.getSelectedQuestionIds().size(), savedChallenge.getId());
                assignSelectedQuestions(savedChallenge, request.getSelectedQuestionIds(), creator);
            }

            // 6. Create initial task for the challenge
            createQuizTask(savedChallenge, creator);

            // 6b. Create progress record for creator (required for challenge completion)
            createCreatorProgress(savedChallenge, creator);

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
     * IMPORTANT: This task is required for challenge completion to work
     */
    private void createQuizTask(Challenge challenge, User creator) {
        log.info("Creating task for quiz challenge ID: {} for creator: {}",
                challenge.getId(), creator.getUsername());

        Task task = new Task();
        task.setChallenge(challenge);
        task.setTitle("Complete Quiz: " + challenge.getTitle());
        task.setDescription("Participate in the quiz challenge and answer all questions");

        // CRITICAL: Set both the User object AND the Long ID
        task.setAssignedToUser(creator);
        task.setAssignedTo(creator.getId());

        // Set task type based on challenge frequency
        task.setType(challenge.getFrequency() != null ?
                TaskType.valueOf(challenge.getFrequency().name()) : TaskType.ONE_TIME);

        // Set verification method from challenge
        task.setVerificationMethod(challenge.getVerificationMethod() != null ?
                challenge.getVerificationMethod() : VerificationMethod.QUIZ);

        // Set status to IN_PROGRESS so it can be found for completion
        task.setStatus(TaskStatus.IN_PROGRESS);

        // Set dates
        task.setStartDate(challenge.getStartDate() != null ?
                challenge.getStartDate() : LocalDateTime.now());
        task.setEndDate(challenge.getEndDate());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        Task savedTask = taskRepository.save(task);
        log.info("Quiz task created successfully with ID: {} for challenge: {}",
                savedTask.getId(), challenge.getId());
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
                        .questionType(questionRequest.getQuestionType() != null ? questionRequest.getQuestionType() : QuestionType.TEXT)
                        .questionMediaUrl(questionRequest.getQuestionMediaUrl())
                        .questionMediaId(questionRequest.getQuestionMediaId())
                        .questionMediaType(questionRequest.getQuestionMediaType())
                        .mediaSourceType(questionRequest.getMediaSourceType())
                        .externalMediaUrl(questionRequest.getExternalMediaUrl())
                        .questionVideoStartTime(questionRequest.getQuestionVideoStartTime())
                        .questionVideoEndTime(questionRequest.getQuestionVideoEndTime())
                        .answerMediaUrl(questionRequest.getAnswerMediaUrl())
                        .answerVideoStartTime(questionRequest.getAnswerVideoStartTime())
                        .answerVideoEndTime(questionRequest.getAnswerVideoEndTime())
                        .answerTextVerification(questionRequest.getAnswerTextVerification())
                        .build();

                log.info("Saving question: text='{}', type={}, hasExternalMedia={}, hasMediaId={}",
                        questionRequest.getQuestion().substring(0, Math.min(50, questionRequest.getQuestion().length())),
                        questionRequest.getQuestionType(),
                        questionRequest.getExternalMediaUrl() != null,
                        questionRequest.getQuestionMediaId() != null);

                QuizQuestion savedQuestion = quizQuestionRepository.save(question);
                log.debug("Saved question ID: {} - {}", savedQuestion.getId(),
                        savedQuestion.getQuestion().substring(0, Math.min(50, savedQuestion.getQuestion().length())));

                // Create the assignment record
                ChallengeQuestionAssignment assignment = ChallengeQuestionAssignment.builder()
                        .challenge(challengeRepository.getReferenceById(challengeId))
                        .question(savedQuestion)
                        .assignmentType(AssignmentType.CREATED_INLINE)
                        .sortOrder(questions.size()) // use current size as sort order
                        .assignedBy(creator)
                        .build();
                challengeQuestionAssignmentRepository.save(assignment);

                questions.add(QuizQuestionMapper.INSTANCE.toDTO(savedQuestion));
            } catch (Exception e) {
                log.error("Error saving question: {}", questionRequest.getQuestion(), e);
                // Continue with other questions even if one fails
            }
        }

        log.info("Finished saving custom questions");
        return questions;
    }

    private void assignSelectedQuestions(Challenge challenge, List<Long> questionIds, User assignedBy) {
        List<QuizQuestion> existingQuestions = quizQuestionRepository.findAllById(questionIds);

        if (existingQuestions.isEmpty()) {
            log.warn("None of the provided question IDs were found for challenge {}: {}", challenge.getId(), questionIds);
            return;
        }

        if (existingQuestions.size() < questionIds.size()) {
            log.warn("Some question IDs were not found. Requested: {}, Found: {}",
                    questionIds.size(), existingQuestions.size());
        }

        List<ChallengeQuestionAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < existingQuestions.size(); i++) {
            // Skip if already assigned (e.g., question was both selected AND created inline)
            if (challengeQuestionAssignmentRepository.existsByChallengeIdAndQuestionId(
                    challenge.getId(), existingQuestions.get(i).getId())) {
                log.debug("Question {} already assigned to challenge {}, skipping",
                        existingQuestions.get(i).getId(), challenge.getId());
                continue;
            }

            assignments.add(ChallengeQuestionAssignment.builder()
                    .challenge(challenge)
                    .question(existingQuestions.get(i))
                    .assignmentType(AssignmentType.SELECTED)
                    .sortOrder(i)
                    .assignedBy(assignedBy)
                    .build());
        }

        if (!assignments.isEmpty()) {
            challengeQuestionAssignmentRepository.saveAll(assignments);
            log.info("Successfully assigned {} SELECTED questions to challenge {}",
                    assignments.size(), challenge.getId());
        }
    }


    /**
     * Get quiz questions for a challenge.
     *
     * Priority order:
     * 1. Questions from the latest COMPLETED session (source of truth for played quests)
     * 2. Questions from junction table (Layer 1 — assigned questions)
     * 3. Questions from legacy source pattern (inline-created)
     * 4. Check IN_PROGRESS sessions (for sessions that were started)
     * 5. Random app questions by difficulty (fallback for brand-new quests)
     */
    public List<QuizQuestionDTO> getQuestionsForChallenge(Long challengeId, QuizDifficulty difficulty, int count) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        // ─── PRIORITY 1: Questions from completed sessions ───
        try {
            List<QuizQuestion> playedQuestions = quizRoundRepository
                    .findQuestionsFromLatestCompletedSession(challengeId);

            if (!playedQuestions.isEmpty()) {
                log.info("Challenge {}: Found {} played questions from completed session",
                        challengeId, playedQuestions.size());
                return playedQuestions.stream()
                        .map(q -> dtoEnricher.enrichWithUrls(QuizQuestionMapper.INSTANCE.toDTO(q)))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching played questions for challenge {}. Falling through.", challengeId, e);
        }

        // ─── PRIORITY 2: Junction table assignments (Layer 1) ───
        List<QuizQuestion> assignedQuestions = challengeQuestionAssignmentRepository
                .findQuestionsByChallengeId(challengeId);
        if (!assignedQuestions.isEmpty()) {
            log.info("Challenge {}: Found {} assigned questions from junction table",
                    challengeId, assignedQuestions.size());
            return assignedQuestions.stream()
                    .map(q -> dtoEnricher.enrichWithUrls(QuizQuestionMapper.INSTANCE.toDTO(q)))
                    .collect(Collectors.toList());
        }

        // ─── PRIORITY 3: Legacy source pattern (inline-created) ───
        List<QuizQuestion> userQuestions = quizQuestionRepository
                .findByCreator_IdAndSourceContaining(
                        challenge.getCreator().getId(),
                        "USER_CREATED_FOR_CHALLENGE_" + challengeId);

        if (!userQuestions.isEmpty()) {
            log.info("Challenge {}: Found {} legacy inline questions",
                    challengeId, userQuestions.size());
            List<QuizQuestionDTO> questions = userQuestions.stream()
                    .map(q -> dtoEnricher.enrichWithUrls(QuizQuestionMapper.INSTANCE.toDTO(q)))
                    .collect(Collectors.toList());

            // Supplement with app questions if needed
            if (questions.size() < count) {
                int remainingCount = count - questions.size();
                List<QuizQuestion> appQuestions = quizQuestionRepository
                        .findByDifficulty(difficulty, PageRequest.of(0, remainingCount));
                questions.addAll(appQuestions.stream()
                        .map(q -> dtoEnricher.enrichWithUrls(QuizQuestionMapper.INSTANCE.toDTO(q)))
                        .collect(Collectors.toList()));
            }
            return questions;
        }

        // ─── PRIORITY 4: Check IN_PROGRESS sessions ───
        Optional<QuizSession> activeSession = quizSessionRepository
                .findFirstByChallengeIdAndStatusOrderByCreatedAtDesc(challengeId, QuizSessionStatus.IN_PROGRESS);

        if (activeSession.isPresent()) {
            List<QuizQuestion> activeQuestions = quizRoundRepository
                    .findQuestionsBySessionId(activeSession.get().getId());
            if (!activeQuestions.isEmpty()) {
                log.info("Challenge {}: Found {} questions from active session {}",
                        challengeId, activeQuestions.size(), activeSession.get().getId());
                return activeQuestions.stream()
                        .map(q -> dtoEnricher.enrichWithUrls(QuizQuestionMapper.INSTANCE.toDTO(q)))
                        .collect(Collectors.toList());
            }
        }

        // ─── PRIORITY 5: Random fallback (only for brand-new quests with zero history) ───
        log.warn("Challenge {}: No question source found. Falling back to {} random questions.",
                challengeId, count);
        List<QuizQuestion> appQuestions = quizQuestionRepository
                .findByDifficulty(difficulty, PageRequest.of(0, count));
        return appQuestions.stream()
                .map(q -> dtoEnricher.enrichWithUrls(QuizQuestionMapper.INSTANCE.toDTO(q)))
                .collect(Collectors.toList());
    }

    /**
     * Get the actually-played questions for a challenge with full round context.
     * This is the "review mode" data — shows what was played, what was answered, etc.
     *
     * @param challengeId The challenge ID
     * @param sessionId   Optional specific session. If null, uses latest COMPLETED session.
     * @return List of round DTOs with embedded question data
     */
    public List<QuizRoundDTO> getPlayedQuestionsForChallenge(Long challengeId, Long sessionId) {
        challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        Long targetSessionId;

        if (sessionId != null) {
            // Use the specified session
            QuizSession session = quizSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));
            if (!session.getChallenge().getId().equals(challengeId)) {
                throw new IllegalArgumentException("Session does not belong to this challenge");
            }
            targetSessionId = sessionId;
        } else {
            // Find the latest completed session for this challenge
            Optional<QuizSession> latestCompleted = quizSessionRepository
                    .findFirstByChallengeIdAndStatusOrderByCompletedAtDesc(
                            challengeId, QuizSessionStatus.COMPLETED);

            if (latestCompleted.isEmpty()) {
                log.info("No completed sessions found for challenge {}. Returning empty.", challengeId);
                return List.of();
            }
            targetSessionId = latestCompleted.get().getId();
        }

        log.info("Fetching played questions for challenge {} from session {}", challengeId, targetSessionId);

        List<QuizRound> rounds = quizRoundRepository.findByQuizSessionIdOrderByRoundNumber(targetSessionId);

        return rounds.stream()
                .map(this::convertRoundToDTO)
                .collect(Collectors.toList());
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
                .enableAiAnswerValidation(sessionConfig.getEnableAiAnswerValidation() != null ? sessionConfig.getEnableAiAnswerValidation() : false)
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

            QuizRound savedRound = quizRoundRepository.save(round);

            // Initialize Brain Ring state if in BRAIN_RING mode
            if (session.getGameMode() == GameMode.BRAIN_RING) {
                brainRingService.initializeRoundState(savedRound);
            }

            // Increment usage count for the question
            question.setUsageCount((question.getUsageCount() == null ? 0 : question.getUsageCount()) + 1);
            quizQuestionRepository.save(question);
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
                .quizConfig(challenge.getQuizConfig()) // This contains the full JSON with all fields
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
                .question(dtoEnricher.enrichWithUrls(QuizQuestionMapper.INSTANCE.toDTO(round.getQuestion())))  // FIXED: was .questionId() and .question()
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

    private void createCreatorProgress(Challenge challenge, User creator) {
        log.info("Creating progress record for challenge ID: {} and creator: {}",
                challenge.getId(), creator.getUsername());

        ChallengeProgress progress = new ChallengeProgress();
        progress.setChallenge(challenge);
        progress.setUser(creator);
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        progress.setCompletionPercentage(0.0);
        progress.setCreatedAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());

        challengeProgressRepository.save(progress);

        log.info("Progress record created successfully for challenge {}", challenge.getId());
    }

    /**
     * Get all quiz sessions for a challenge by a user (Session History)
     */
    @Transactional(readOnly = true)
    public Page<QuizSessionSummaryDTO> getSessionHistory(Long challengeId, Long userId, Pageable pageable) {
        log.info("Fetching session history for challenge: {}, user: {}", challengeId, userId);

        // 1. Verify user has access to challenge
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        if (!isUserChallengeParticipant(challengeId, userId)) {
            throw new IllegalStateException("Access denied to challenge history");
        }

        // 2. Query sessions for this challenge by this user
        Page<QuizSession> sessions = quizSessionRepository.findByChallengeIdAndHostUserIdOrderByCreatedAtDesc(challengeId, userId, pageable);

        // 3. Map to DTO
        return sessions.map(session -> QuizSessionSummaryDTO.builder()
                .sessionId(session.getId())
                .correctAnswers(session.getCorrectAnswers())
                .totalRounds(session.getTotalRounds())
                .scorePercentage(session.getScorePercentage())
                .status(session.getStatus().name())
                .questionSource(session.getQuestionSource() != null ? session.getQuestionSource().name() : null)
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .duration(session.getTotalDurationSeconds() != null ? session.getTotalDurationSeconds().longValue() : null)
                .build());
    }

    /**
     * Override createQuizRounds to prioritize questions from the junction table
     * if the session is linked to a challenge.
     */
    @Override
    protected void createQuizRounds(QuizSession session, StartQuizSessionRequest request) {
        if (request.getChallengeId() != null) {
            log.info("Creating rounds for session {} using questions assigned to challenge {}",
                    session.getId(), request.getChallengeId());

            // Get questions for this specific challenge via junction table
            List<QuizQuestion> assignedQuestions = challengeQuestionAssignmentRepository
                    .findQuestionsByChallengeId(request.getChallengeId());

            if (!assignedQuestions.isEmpty()) {
                log.info("Found {} assigned questions for challenge {}. Using them for session {}.",
                        assignedQuestions.size(), request.getChallengeId(), session.getId());

                // Create rounds using these questions
                int roundsToCreate = Math.min(session.getTotalRounds(), assignedQuestions.size());
                for (int i = 0; i < roundsToCreate; i++) {
                    QuizRound round = QuizRound.builder()
                            .quizSession(session)
                            .question(assignedQuestions.get(i))
                            .roundNumber(i + 1)
                            .isCorrect(false)
                            .hintUsed(false)
                            .voiceRecordingUsed(false)
                            .build();
                    QuizRound savedRound = quizRoundRepository.save(round);

                    // Initialize Brain Ring state if in BRAIN_RING mode
                    if (session.getGameMode() == GameMode.BRAIN_RING) {
                        brainRingService.initializeRoundState(savedRound);
                    }

                    // Increment usage count
                    QuizQuestion q = assignedQuestions.get(i);
                    q.setUsageCount(q.getUsageCount() + 1);
                    quizQuestionRepository.save(q);
                }
                return;
            }
            log.warn("No questions assigned to challenge {} via junction table. Falling back to default logic.",
                    request.getChallengeId());
        }

        // Default logic from parent QuizService
        super.createQuizRounds(session, request);
    }

    /**
     * Replay a quiz challenge by creating a new session
     */
    @Transactional
    public QuizSessionDTO replayChallenge(Long challengeId, Long userId, ReplayChallengeRequest request) {
        log.info("Replaying challenge: {} for user: {}", challengeId, userId);

        // 1. Load the challenge, verify it exists and is QUIZ type
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        if (challenge.getType() != ChallengeType.QUIZ) {
            throw new IllegalArgumentException("Challenge is not a quiz type");
        }

        // 2. Verify user is creator or participant
        if (!isUserChallengeParticipant(challengeId, userId)) {
            throw new IllegalStateException("Access denied to replay challenge");
        }

        // 3. Parse existing quizConfig from challenge
        QuizSessionConfig sessionConfig = new QuizSessionConfig();
        try {
            if (challenge.getQuizConfig() != null) {
                QuizChallengeConfig challengeConfig = objectMapper.readValue(challenge.getQuizConfig(), QuizChallengeConfig.class);

                sessionConfig.setDifficulty(challengeConfig.getDefaultDifficulty() != null ?
                        challengeConfig.getDefaultDifficulty() : QuizDifficulty.MEDIUM);
                sessionConfig.setTotalRounds(challengeConfig.getDefaultTotalRounds() != null ?
                        challengeConfig.getDefaultTotalRounds() : 10);
                sessionConfig.setRoundTimeSeconds(challengeConfig.getDefaultRoundTimeSeconds() != null ?
                        challengeConfig.getDefaultRoundTimeSeconds() : 30);
                sessionConfig.setEnableAiHost(challengeConfig.getEnableAiHost() != null ?
                        challengeConfig.getEnableAiHost() : false);
                sessionConfig.setEnableAiAnswerValidation(challengeConfig.getEnableAiAnswerValidation() != null ?
                        challengeConfig.getEnableAiAnswerValidation() : false);
                
                String qSource = challengeConfig.getQuestionSource();
                if (qSource != null) {
                    try {
                        sessionConfig.setQuestionSource(QuestionSource.valueOf(qSource.toLowerCase()));
                    } catch (Exception e) {
                        sessionConfig.setQuestionSource(QuestionSource.app);
                    }
                } else {
                    sessionConfig.setQuestionSource(QuestionSource.app);
                }
                
                sessionConfig.setTeamName(challengeConfig.getTeamName());
                sessionConfig.setTeamMembers(challengeConfig.getTeamMembers() != null ?
                        challengeConfig.getTeamMembers() : new ArrayList<>());
            }
        } catch (Exception e) {
            log.error("Failed to parse quiz config from challenge", e);
            // Fallback to defaults
            sessionConfig.setDifficulty(QuizDifficulty.MEDIUM);
            sessionConfig.setTotalRounds(10);
            sessionConfig.setRoundTimeSeconds(30);
            sessionConfig.setQuestionSource(QuestionSource.app);
        }

        // 4. Apply overrides from ReplayChallengeRequest
        if (request != null) {
            if (request.getDifficulty() != null) {
                try {
                    sessionConfig.setDifficulty(QuizDifficulty.valueOf(request.getDifficulty().toUpperCase()));
                } catch (Exception e) {
                    log.warn("Invalid difficulty override: {}", request.getDifficulty());
                }
            }
            if (request.getRoundCount() != null) {
                sessionConfig.setTotalRounds(request.getRoundCount());
            }
            if (request.getRoundTime() != null) {
                sessionConfig.setRoundTimeSeconds(request.getRoundTime());
            }
            if (request.getEnableAIHost() != null) {
                sessionConfig.setEnableAiHost(request.getEnableAIHost());
            }
            if (request.getEnableAiAnswerValidation() != null) {
                sessionConfig.setEnableAiAnswerValidation(request.getEnableAiAnswerValidation());
            }
            if (request.getQuestionSource() != null) {
                try {
                    sessionConfig.setQuestionSource(QuestionSource.valueOf(request.getQuestionSource().toLowerCase()));
                } catch (Exception e) {
                    log.warn("Invalid question source override: {}", request.getQuestionSource());
                }
            }
        }

        // 5. Create a new QuizSession using the merged config
        return createQuizSessionForChallenge(challengeId, userId, sessionConfig);
    }
}