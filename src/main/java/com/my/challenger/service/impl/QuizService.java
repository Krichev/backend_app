package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.HintResponseDTO;
import com.my.challenger.dto.SessionStatsDTO;
import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.*;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.QuizRound;
import com.my.challenger.entity.quiz.QuizSession;
import com.my.challenger.entity.quiz.Topic;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.mapper.QuizQuestionMapper;
import com.my.challenger.repository.*;
import com.my.challenger.service.WWWGameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuizService {

    protected final QuizQuestionRepository quizQuestionRepository;
    protected final QuizSessionRepository quizSessionRepository;
    protected final QuizRoundRepository quizRoundRepository;
    protected final ChallengeRepository challengeRepository;
    protected final UserRepository userRepository;
    protected final MediaFileRepository mediaFileRepository;
    protected final QuestRepository questRepository;
    protected final WWWGameService gameService;
    protected final MinioMediaStorageService mediaStorageService;
    protected final TopicService topicService;

    public QuizService(
            QuizQuestionRepository quizQuestionRepository,
            QuizSessionRepository quizSessionRepository,
            QuizRoundRepository quizRoundRepository,
            ChallengeRepository challengeRepository,
            UserRepository userRepository,
            MediaFileRepository mediaFileRepository,
            QuestRepository questRepository,
            WWWGameService gameService,
            MinioMediaStorageService mediaStorageService,
            TopicService topicService) {
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizSessionRepository = quizSessionRepository;
        this.quizRoundRepository = quizRoundRepository;
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.questRepository = questRepository;
        this.gameService = gameService;
        this.mediaStorageService = mediaStorageService;
        this.topicService = topicService;
    }

    /**
     * Get quiz session by ID
     */
    @Transactional(readOnly = true)
    public QuizSessionDTO getQuizSession(Long sessionId, Long userId) {
        log.debug("Getting quiz session: {} for user: {}", sessionId, userId);
        
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz session not found with id: " + sessionId));
        
        // Validate user has access to this session
        if (!session.getHostUser().getId().equals(userId)) {
            log.warn("User {} attempted to access session {} owned by user {}", 
                    userId, sessionId, session.getHostUser().getId());
            throw new AccessDeniedException("You don't have access to this quiz session");
        }
        
        return convertSessionToDTO(session);
    }

    // =============================================================================
    // QUESTION MANAGEMENT METHODS
    // =============================================================================

    @Transactional
    public QuizQuestionDTO createUserQuestion(CreateQuizQuestionRequest request, Long userId) {
        log.info("Creating user question for user: {}", userId);

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

        Topic topic = null;
        if (request.getTopic() != null && !request.getTopic().isBlank()) {
            topic = topicService.getOrCreateTopic(request.getTopic());
        }

        // Determine question type based on media
        QuestionType questionType = request.getQuestionType();
        MediaType mediaType = null;
        String mediaUrl = null;
        Long mediaId = null;
        String thumbnailUrl = null;

        // Handle media file if provided
        if (request.getMediaFileId() != null) {
            MediaFile mediaFile = mediaFileRepository.findById(request.getMediaFileId())
                    .orElseThrow(() -> new IllegalArgumentException("Media file not found"));

            mediaUrl = mediaStorageService.getMediaUrl(mediaFile);
            mediaId = mediaFile.getId();
            mediaType = mediaFile.getMediaType();
            thumbnailUrl = mediaFile.getThumbnailPath() != null ?
                    mediaStorageService.getThumbnailUrl(mediaFile) : null;

            // Auto-set question type based on media type if not explicitly set
            if (questionType == QuestionType.TEXT) {
                questionType = mapMediaTypeToQuestionType(mediaType);
            }
        } else if (request.getQuestionMediaUrl() != null) {
            // Use provided URL directly
            mediaUrl = request.getQuestionMediaUrl();
            mediaId = request.getQuestionMediaId();
            mediaType = request.getQuestionMediaType();
        }

        QuizQuestion question = QuizQuestion.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .difficulty(request.getDifficulty())
                .topic(topic)
                .source(request.getSource())
                .additionalInfo(request.getAdditionalInfo())
                .questionType(questionType)
                .questionMediaUrl(mediaUrl)
                .questionMediaId(mediaId)
                .questionMediaType(mediaType)
                .questionThumbnailUrl(thumbnailUrl)
                .isUserCreated(true)
                .creator(creator)
                .usageCount(0)
                .isActive(true)
                .build();

        QuizQuestion saved = quizQuestionRepository.save(question);
        log.info("Created question with ID: {} and type: {}", saved.getId(), saved.getQuestionType());
        return QuizQuestionMapper.INSTANCE.toDTO(saved);
    }

    private QuestionType mapMediaTypeToQuestionType(MediaType mediaType) {
        if (mediaType == null) {
            return QuestionType.TEXT;
        }
        switch (mediaType) {
            case IMAGE:
                return QuestionType.IMAGE;
            case VIDEO:
                return QuestionType.VIDEO;
            case AUDIO:
                return QuestionType.AUDIO;
            default:
                return QuestionType.TEXT;
        }
    }

    public List<QuizQuestionDTO> getUserQuestions(Long userId) {
        log.info("Getting questions for user: {}", userId);
        List<QuizQuestion> questions = quizQuestionRepository.findByCreator_IdAndIsUserCreated(userId, true);
        return questions.stream()
                .map(QuizQuestionMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUserQuestion(Long questionId, Long userId) {
        log.info("Deleting question {} for user: {}", questionId, userId);

        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        if (!question.getCreator().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own questions");
        }

        if (!question.getIsUserCreated()) {
            throw new IllegalArgumentException("Cannot delete system questions");
        }

        quizQuestionRepository.delete(question);
        log.info("Deleted question: {}", questionId);
    }

    public List<QuizQuestionDTO> getQuestionsByDifficulty(QuizDifficulty difficulty) {
        log.info("Getting questions by difficulty: {}", difficulty);
        List<QuizQuestion> questions = quizQuestionRepository.findByDifficulty(difficulty);
        return questions.stream()
                .map(QuizQuestionMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());
    }

    // =============================================================================
    // SESSION MANAGEMENT METHODS
    // =============================================================================

    @Transactional
    public QuizSessionDTO startQuizSession(StartQuizSessionRequest request, Long hostUserId) {
        log.info("Starting quiz session for user: {}", hostUserId);

        User host = userRepository.findById(hostUserId)
                .orElseThrow(() -> new IllegalArgumentException("Host user not found"));

        // Create new session
        QuizSession session = QuizSession.builder()
                .hostUser(host)
                .creatorId(hostUserId)
                .userId(hostUserId)
                .status(QuizSessionStatus.CREATED)
                .difficulty(request.getDifficulty())
                .totalRounds(request.getTotalRounds())
                .completedRounds(0)
                .correctAnswers(0)
                .roundTimeSeconds(request.getRoundTimeSeconds())
                .teamName(request.getTeamName())
                .teamMembers(String.join(",", request.getTeamMembers()))
                .enableAiHost(request.getEnableAiHost() != null ? request.getEnableAiHost() : false)
                .questionSource(request.getQuestionSource())
                .build();

        // Handle challenge linkage if provided
        if (request.getChallengeId() != null) {
            Challenge challenge = challengeRepository.findById(request.getChallengeId())
                    .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));
            session.setChallenge(challenge);
        }

        QuizSession savedSession = quizSessionRepository.save(session);

        // Create rounds for the session
        createQuizRounds(savedSession, request);

        log.info("Started quiz session with ID: {}", savedSession.getId());
        return convertSessionToDTO(savedSession);
    }

    private void createQuizRounds(QuizSession session, StartQuizSessionRequest request) {
        log.info("Creating quiz rounds for session: {} using source: {}", session.getId(), request.getQuestionSource());

        List<QuizQuestion> questions;

        if (QuestionSource.user.equals(request.getQuestionSource()) && request.getCustomQuestionIds() != null && !request.getCustomQuestionIds().isEmpty()) {
            log.info("Using {} custom questions for session {}", request.getCustomQuestionIds().size(), session.getId());
            questions = quizQuestionRepository.findAllById(request.getCustomQuestionIds());
            if (questions.size() < request.getTotalRounds()) {
                log.error("Insufficient custom questions: provided {}, required {}", questions.size(), request.getTotalRounds());
                throw new IllegalArgumentException("Not enough custom questions provided. Required: " + request.getTotalRounds() + ", provided: " + questions.size());
            }
        } else {
            log.info("Fetching {} random questions by difficulty: {} for session {}", 
                    request.getTotalRounds(), request.getDifficulty(), session.getId());
            
            PageRequest pageRequest = PageRequest.of(0, request.getTotalRounds());
            questions = quizQuestionRepository.findByDifficultyOrderByUsageCountAsc(
                    request.getDifficulty(), pageRequest);
            
            if (questions.isEmpty()) {
                log.error("No questions found in database for difficulty {}", request.getDifficulty());
                throw new IllegalArgumentException("No questions available for the selected difficulty (" + request.getDifficulty() + ")");
            }

            if (questions.size() < request.getTotalRounds()) {
                log.warn("Insufficient questions in database for difficulty {}: found {}, required {}. Proceeding with available questions.",
                        request.getDifficulty(), questions.size(), request.getTotalRounds());
            }
        }

        // Create rounds
        int roundsToCreate = Math.min(request.getTotalRounds(), questions.size());
        for (int i = 0; i < roundsToCreate; i++) {
            QuizRound round = QuizRound.builder()
                    .quizSession(session)
                    .question(questions.get(i))
                    .roundNumber(i + 1)
                    .hintUsed(false)
                    .voiceRecordingUsed(false)
                    .isCorrect(false)
                    .build();
            quizRoundRepository.save(round);

            // Increment usage count for the question
            QuizQuestion question = questions.get(i);
            question.setUsageCount(question.getUsageCount() + 1);
            quizQuestionRepository.save(question);
        }
        log.info("Successfully created {} rounds for session {}", roundsToCreate, session.getId());
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
    public QuizRoundDTO submitRoundAnswerById(Long sessionId, Long roundId, SubmitRoundAnswerByIdRequest request, Long userId) {
        log.info("Submitting answer for session {} round ID {} by user: {}", sessionId, roundId, userId);
        
        // Verify user has access to session
        QuizSession session = findUserSession(sessionId, userId);
        
        if (session.getStatus() != QuizSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session must be in progress to submit answers");
        }
        
        // Find round by ID
        QuizRound round = quizRoundRepository.findById(roundId)
            .orElseThrow(() -> new IllegalArgumentException("Round not found with id: " + roundId));
        
        // Verify round belongs to this session
        if (!round.getQuizSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("Round " + roundId + " does not belong to session " + sessionId);
        }
        
        // Check if already submitted
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
        round.setPlayerWhoAnswered(request.getPlayerWhoAnswered() != null ? request.getPlayerWhoAnswered() : "Team");
        round.setDiscussionNotes(request.getDiscussionNotes());
        round.setHintUsed(request.getHintUsed() != null ? request.getHintUsed() : false);
        round.setVoiceRecordingUsed(request.getVoiceRecordingUsed() != null ? request.getVoiceRecordingUsed() : false);
        round.setAnswerSubmittedAt(LocalDateTime.now());
        
        // Generate AI feedback if enabled
        if (session.getEnableAiHost() != null && session.getEnableAiHost()) {
            String feedback = gameService.generateRoundFeedback(round, isCorrect);
            round.setAiFeedback(feedback);
        }
        
        QuizRound savedRound = quizRoundRepository.save(round);
        
        // Update session progress
        updateSessionProgress(session);
        
        return convertRoundToDTO(savedRound);
    }

    private void updateSessionProgress(QuizSession session) {
        // Find all rounds for this session
        List<QuizRound> rounds = quizRoundRepository.findByQuizSessionIdOrderByRoundNumber(session.getId());
        
        // Count completed rounds and correct answers
        long completedCount = rounds.stream()
                .filter(r -> r.getAnswerSubmittedAt() != null)
                .count();
        
        long correctCount = rounds.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                .count();
        
        session.setCompletedRounds((int) completedCount);
        session.setCorrectAnswers((int) correctCount);
        
        // Check if all rounds are completed
        if (completedCount >= session.getTotalRounds()) {
            completeSession(session);
        }
        
        quizSessionRepository.save(session);
    }

    @Transactional
    public QuizRoundResultDTO submitAnswer(SubmitAnswerRequest request, Long userId) {
        log.info("Submitting answer for round {} by user: {}", request.getRoundId(), userId);

        QuizRound round = quizRoundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        QuizSession session = round.getQuizSession();

        // Verify user has access to this session
        if (!session.getHostUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You don't have access to this session");
        }

        // Check if answer was already submitted
        if (round.getAnswerSubmittedAt() != null) {
            throw new IllegalStateException("Answer already submitted for this round");
        }

        // Validate and score the answer
        boolean isCorrect = gameService.validateAnswer(request.getAnswer(), round.getQuestion().getAnswer());

        // Update round
        round.setTeamAnswer(request.getAnswer());
        round.setAnswerSubmittedAt(LocalDateTime.now());
        round.setIsCorrect(isCorrect);
        round.setPlayerWhoAnswered(request.getPlayerWhoAnswered());
        round.setDiscussionNotes(request.getDiscussionNotes());

        if (request.getTimeToAnswer() != null) {
            round.setTotalRoundDurationSeconds(request.getTimeToAnswer());
        }

        if (request.getHintUsed() != null) {
            round.setHintUsed(request.getHintUsed());
        }

        if (request.getVoiceRecordingUsed() != null) {
            round.setVoiceRecordingUsed(request.getVoiceRecordingUsed());
        }

        // Update session statistics
        if (isCorrect) {
            session.setCorrectAnswers(session.getCorrectAnswers() + 1);
        }
        session.setCompletedRounds(session.getCompletedRounds() + 1);

        quizRoundRepository.save(round);

        // Check if session is complete
        if (session.getCompletedRounds() >= session.getTotalRounds()) {
            completeSession(session);
        }

        quizSessionRepository.save(session);

        // Generate AI feedback
        String feedback = gameService.generateRoundFeedback(round, isCorrect);
        round.setAiFeedback(feedback);
        quizRoundRepository.save(round);

        return QuizRoundResultDTO.builder()
                .roundId(round.getId())
                .isCorrect(isCorrect)
                .correctAnswer(round.getQuestion().getAnswer())
                .feedback(feedback)
                .sessionScore(session.getCorrectAnswers())
                .isSessionComplete(session.getStatus() == QuizSessionStatus.COMPLETED)
                .build();
    }

    private void completeSession(QuizSession session) {
        // Calculate score percentage before validation
        Double scorePercentage = calculateScorePercentage(session);

        // Validate minimum score requirement if quest has audio config
        Challenge challenge = session.getChallenge();
        if (challenge != null) {
            // Check if challenge has associated quests with minimum score requirements
            // Note: Challenges can have multiple quests via challenge_quests junction table
            // For now, we validate against the first quest found with a minimum score requirement
            questRepository.findAll().stream()
                    .filter(quest -> quest.getMinimumScorePercentage() != null && quest.getMinimumScorePercentage() > 0)
                    .findFirst()
                    .ifPresent(quest -> {
                        Integer minScore = quest.getMinimumScorePercentage();
                        if (scorePercentage < minScore) {
                            log.warn("❌ Score {}% is below minimum required {}% for quest ID: {}",
                                    scorePercentage, minScore, quest.getId());
                            throw new IllegalStateException(
                                    String.format("Score %.1f%% is below minimum required %d%% to complete this quest",
                                            scorePercentage, minScore));
                        }
                        log.info("✅ Score {}% meets minimum requirement {}% for quest ID: {}",
                                scorePercentage, minScore, quest.getId());
                    });
        }

        // Set completion status
        session.setStatus(QuizSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());

        if (session.getStartedAt() != null) {
            long duration = java.time.Duration.between(session.getStartedAt(), session.getCompletedAt()).getSeconds();
            session.setTotalDurationSeconds((int) duration);
        }
    }

    @Transactional
    public HintResponseDTO useHint(Long roundId, Long userId) {
        log.info("Using hint for round {} by user: {}", roundId, userId);

        QuizRound round = quizRoundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        QuizSession session = round.getQuizSession();

        // Verify user has access
        if (!session.getHostUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You don't have access to this session");
        }

        // Check if hint was already used
        if (round.getHintUsed()) {
            throw new IllegalStateException("Hint already used for this round");
        }

        // Mark hint as used
        round.setHintUsed(true);
        quizRoundRepository.save(round);

        // Generate hint
        String hint = gameService.generateHint(
                round.getQuestion().getAnswer(),
                round.getQuestion().getDifficulty().toString()
        );

        return HintResponseDTO.builder()
                .hint(hint)
                .roundId(roundId)
                .build();
    }

    public QuizSessionDTO getSessionDetails(Long sessionId, Long userId) {
        log.info("Getting session {} details for user: {}", sessionId, userId);
        QuizSession session = findUserSession(sessionId, userId);
        return convertSessionToDTO(session);
    }

    public List<QuizSessionDTO> getUserSessions(Long userId, QuizSessionStatus status) {
        log.info("Getting sessions for user {} with status: {}", userId, status);

        List<QuizSession> sessions;
        if (status != null) {
            sessions = quizSessionRepository.findByHostUserIdAndStatus(userId, status);
        } else {
            sessions = quizSessionRepository.findByHostUserId(userId);
        }

        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }

    public QuizStatsDTO getUserQuizStats(Long userId) {
        log.info("Getting quiz stats for user: {}", userId);

        List<QuizSession> completedSessions = quizSessionRepository
                .findByHostUserIdAndStatus(userId, QuizSessionStatus.COMPLETED);

        int totalSessions = completedSessions.size();
        int totalScore = completedSessions.stream()
                .mapToInt(QuizSession::getCorrectAnswers)
                .sum();

        double averageScore = totalSessions > 0 ? (double) totalScore / totalSessions : 0;

        // Get completion rate
        long allSessions = quizSessionRepository.countByHostUserId(userId);
        double completionRate = allSessions > 0 ? (double) totalSessions / allSessions * 100 : 0;

        return QuizStatsDTO.builder()
                .totalSessions(totalSessions)
                .totalScore(totalScore)
                .averageScore(averageScore)
                .completionRate(completionRate)
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

        QuizSession updated = quizSessionRepository.save(session);
        return convertSessionToDTO(updated);
    }

    @Transactional
    public QuizSessionDTO pauseSession(Long sessionId, PauseQuizSessionRequest request, Long userId) {
        log.info("Pausing session {} for user: {}", sessionId, userId);

        QuizSession session = findUserSession(sessionId, userId);

        if (session.getStatus() != QuizSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only IN_PROGRESS sessions can be paused. Current status: " + session.getStatus());
        }

        session.setStatus(QuizSessionStatus.PAUSED);
        session.setPausedAt(LocalDateTime.now());
        session.setPausedAtRound(request.getPausedAtRound());
        session.setRemainingTimeSeconds(request.getRemainingTimeSeconds());
        session.setPausedAnswer(request.getCurrentAnswer());
        session.setPausedNotes(request.getDiscussionNotes());

        QuizSession saved = quizSessionRepository.save(session);
        log.info("Session {} paused successfully", sessionId);
        return convertSessionToDTO(saved);
    }

    @Transactional
    public QuizSessionDTO resumeSession(Long sessionId, Long userId) {
        log.info("Resuming session {} for user: {}", sessionId, userId);

        QuizSession session = findUserSession(sessionId, userId);

        if (session.getStatus() != QuizSessionStatus.PAUSED) {
            throw new IllegalStateException("Only PAUSED sessions can be resumed. Current status: " + session.getStatus());
        }

        session.setStatus(QuizSessionStatus.IN_PROGRESS);
        session.setPausedAt(null); // Clear paused timestamp
        
        QuizSession saved = quizSessionRepository.save(session);
        log.info("Session {} resumed successfully", sessionId);
        return convertSessionToDTO(saved);
    }

    @Transactional
    public void abandonSession(Long sessionId, Long userId) {
        log.info("Abandoning session {} for user: {}", sessionId, userId);

        QuizSession session = findUserSession(sessionId, userId);

        if (session.getStatus() == QuizSessionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot abandon a completed session");
        }

        session.setStatus(QuizSessionStatus.ABANDONED);
        session.setCompletedAt(LocalDateTime.now());

        quizSessionRepository.save(session);
        log.info("Session {} marked as abandoned", sessionId);
    }

    // =============================================================================
    // HELPER METHODS
    // =============================================================================

    private QuizSession findUserSession(Long sessionId, Long userId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getHostUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You don't have access to this session");
        }

        return session;
    }

    protected QuizSessionDTO convertSessionToDTO(QuizSession session) {
        List<String> teamMembers = session.getTeamMembers() != null ?
                List.of(session.getTeamMembers().split(",")) : List.of();

        QuizSessionDTO dto = QuizSessionDTO.builder()
                .id(session.getId())
                .hostUserId(session.getHostUser().getId())
                .hostUsername(session.getHostUser().getUsername())
                .teamName(session.getTeamName())
                .teamMembers(teamMembers)
                .status(session.getStatus())
                .difficulty(session.getDifficulty())
                .totalRounds(session.getTotalRounds())
                .completedRounds(session.getCompletedRounds())
                .correctAnswers(session.getCorrectAnswers())
                .scorePercentage(calculateScorePercentage(session))
                .roundTimeSeconds(session.getRoundTimeSeconds())
                .enableAiHost(session.getEnableAiHost())
                .questionSource(session.getQuestionSource())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .totalDurationSeconds(session.getTotalDurationSeconds())
                .createdAt(session.getCreatedAt())
                .pausedAt(session.getPausedAt())
                .pausedAtRound(session.getPausedAtRound())
                .remainingTimeSeconds(session.getRemainingTimeSeconds())
                .pausedAnswer(session.getPausedAnswer())
                .pausedNotes(session.getPausedNotes())
                .build();

        if (session.getChallenge() != null) {
            dto.setChallengeId(session.getChallenge().getId());
            dto.setChallengeTitle(session.getChallenge().getTitle());
        }

        return dto;
    }

    private QuizRoundDTO convertRoundToDTO(QuizRound round) {
        QuizRoundDTO dto = QuizRoundDTO.builder()
                .id(round.getId())
                .quizSessionId(round.getQuizSession().getId())
                .question(QuizQuestionMapper.INSTANCE.toDTO(round.getQuestion()))
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

        // Don't include the answer unless the round is complete
        if (round.getAnswerSubmittedAt() == null) {
            dto.getQuestion().setAnswer(null);
        }

        return dto;
    }

    private Double calculateScorePercentage(QuizSession session) {
        if (session.getTotalRounds() == null || session.getTotalRounds() == 0) {
            return 0.0;
        }
        return (double) session.getCorrectAnswers() / session.getTotalRounds() * 100;
    }

    /**
     * Update the status of a quiz session
     */
    public QuizSession updateSessionStatus(Long sessionId, QuizSessionStatus status, Long userId) {
        log.debug("Updating session {} status to {}", sessionId, status);

        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz session not found with id: " + sessionId));

        session.setStatus(status);
        session.setUpdatedAt(LocalDateTime.now());

        if (QuizSessionStatus.COMPLETED.equals(status)) {
            session.setCompletedAt(LocalDateTime.now());
        }

        return quizSessionRepository.save(session);
    }

    /**
     * Get statistics for a specific session
     */
    public SessionStatsDTO getSessionStats(Long sessionId) {
        log.debug("Getting stats for session {}", sessionId);

        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz session not found with id: " + sessionId));

        SessionStatsDTO stats = new SessionStatsDTO();
        stats.setSessionId(sessionId);
//        stats.setTotalQuestions(session.getTotalQuestions());
        stats.setCorrectAnswers(session.getCorrectAnswers());
//        stats.setIncorrectAnswers(session.getTotalQuestions() - session.getCorrectAnswers());
//        stats.setScore(session.getScore());
//        stats.setAccuracy(calculateAccuracy(session));
//        stats.setDuration(session.getDuration());
//        stats.setStatus(session.getStatus());
        stats.setQuestionSource(session.getQuestionSource());
        stats.setCreatedAt(session.getCreatedAt());
        stats.setCompletedAt(session.getCompletedAt());

        // Calculate additional stats
//        stats.setAverageTimePerQuestion(calculateAverageTimePerQuestion(session));
//        stats.setPerformanceLevel(determinePerformanceLevel(session));

        return stats;
    }

    /**
     * Get recent quiz sessions with pagination
     */
    public Page<QuizSession> getRecentSessions(int page, int size) {
        log.debug("Getting recent sessions - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return quizSessionRepository.findAll(pageable);
    }

    /**
     * Get sessions by exact question source
     */
    public List<QuizSession> getSessionsByExactQuestionSource(String source) {
        log.debug("Getting sessions with exact question source: {}", source);

        return quizSessionRepository.findByQuestionSource(source);
    }

//    /**
//     * Get all quiz sessions for a specific user
//     */
//    public List<QuizSession> getUserQuizSessions(Long userId) {
//        log.debug("Getting quiz sessions for user: {}", userId);
//
//        User user = userService.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
//
//        return quizSessionRepository.findByUserOrderByCreatedAtDesc(user);
//    }
//
//    /**
//     * Get user quiz sessions with pagination
//     */
//    public Page<QuizSession> getUserQuizSessionsPaged(Long userId, int page, int size) {
//        log.debug("Getting paged quiz sessions for user: {}", userId);
//
//        User user = userService.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
//
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
//        return quizSessionRepository.findByUser(user, pageable);
//    }

//    /**
//     * Get user statistics summary
//     */
//    public Map<String, Object> getUserStatsSummary(Long userId) {
//        List<QuizSession> sessions = getUserQuizSessions(userId);
//
//        Map<String, Object> summary = new HashMap<>();
//        summary.put("totalSessions", sessions.size());
//        summary.put("completedSessions", sessions.stream()
//                .filter(s -> "COMPLETED".equalsIgnoreCase(s.getStatus()))
//                .count());
//
//        if (!sessions.isEmpty()) {
//            double averageScore = sessions.stream()
//                    .filter(s -> s.getScore() != null)
//                    .mapToDouble(QuizSession::getScore)
//                    .average()
//                    .orElse(0.0);
//
//            int totalQuestions = sessions.stream()
//                    .mapToInt(QuizSession::getTotalQuestions)
//                    .sum();
//
//            int totalCorrect = sessions.stream()
//                    .mapToInt(QuizSession::getCorrectAnswers)
//                    .sum();
//
//            summary.put("averageScore", averageScore);
//            summary.put("totalQuestionsAnswered", totalQuestions);
//            summary.put("totalCorrectAnswers", totalCorrect);
//            summary.put("overallAccuracy", totalQuestions > 0 ?
//                    (double) totalCorrect / totalQuestions * 100 : 0.0);
//        }
//
//        return summary;
//    }

    // Helper methods
//    private double calculateAccuracy(QuizSession session) {
//        if (session.getTotalQuestions() == 0) {
//            return 0.0;
//        }
//        return (double) session.getCorrectAnswers() / session.getTotalQuestions() * 100;
//    }
//
//    private Double calculateAverageTimePerQuestion(QuizSession session) {
//        if (session.getDuration() == null || session.getTotalQuestions() == 0) {
//            return null;
//        }
//        return (double) session.getDuration() / session.getTotalQuestions();
//    }

//    private String determinePerformanceLevel(QuizSession session) {
//        double accuracy = calculateAccuracy(session);
//        if (accuracy >= 90) return "EXCELLENT";
//        if (accuracy >= 80) return "VERY_GOOD";
//        if (accuracy >= 70) return "GOOD";
//        if (accuracy >= 60) return "FAIR";
//        return "NEEDS_IMPROVEMENT";
//    }
}