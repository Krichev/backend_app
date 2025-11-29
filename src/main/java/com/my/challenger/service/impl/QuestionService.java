package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.*;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.QuizRound;
import com.my.challenger.entity.quiz.QuizSession;
import com.my.challenger.entity.quiz.Topic;
import com.my.challenger.exception.BadRequestException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.mapper.QuizQuestionMapper;
import com.my.challenger.repository.*;
import com.my.challenger.service.WWWGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.my.challenger.entity.enums.QuestionType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    protected final QuizQuestionRepository quizQuestionRepository;
    protected final QuizSessionRepository quizSessionRepository;
    protected final QuizRoundRepository quizRoundRepository;
    protected final ChallengeRepository challengeRepository;
    protected final UserRepository userRepository;
    protected final WWWGameService gameService;
    protected final TopicService topicService;
    private final QuestionAccessService accessService;
    private final UserRelationshipService relationshipService;
    private final MinioMediaStorageService mediaStorageService;
    private final QuizQuestionDTOEnricher dtoEnricher;

    @Transactional
    public QuizQuestionDTO createQuestionWithMedia(
            CreateQuizQuestionRequest request,
            MultipartFile mediaFile,
            Long userId) {

        // 1. Get user
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

        // 2. Handle media upload (if provided)
        Long mediaFileId = null;
        String mediaS3Key = null;        // CHANGED: Store S3 key, not URL
        String thumbnailS3Key = null;    // CHANGED: Store S3 key, not URL
        MediaType mediaType = null;

        if (mediaFile != null && !mediaFile.isEmpty()) {
            log.info("📤 Uploading media file: {} ({} bytes)",
                    mediaFile.getOriginalFilename(), mediaFile.getSize());

            validateMediaFile(mediaFile);
            MediaFile storedMedia = mediaStorageService.storeMedia(
                    mediaFile, null, MediaCategory.QUIZ_QUESTION, userId);

            mediaFileId = storedMedia.getId();
            mediaS3Key = storedMedia.getS3Key();           // CHANGED: Use S3 key
            thumbnailS3Key = storedMedia.getThumbnailPath(); // CHANGED: Use thumbnail S3 key
            mediaType = storedMedia.getMediaType();

            log.info("✅ Media stored: ID={}, S3Key={}, Type={}", mediaFileId, mediaS3Key, mediaType);
        }

        // 3. Get/create topic
        Topic topic = null;
        if (request.getTopic() != null && !request.getTopic().isBlank()) {
            topic = topicService.getOrCreateTopic(request.getTopic());
        }

        // 4. Auto-detect question type from media
        QuestionType questionType = request.getQuestionType();
        if ((questionType == null || questionType == TEXT) && mediaType != null) {
            questionType = mapMediaTypeToQuestionType(mediaType);
            log.info("Auto-detected question type: {}", questionType);
        }

        // 5. Build and save question - STORE S3 KEYS, NOT URLS
        QuizQuestion question = QuizQuestion.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : QuizDifficulty.MEDIUM)
                .topic(topic)
                .questionType(questionType != null ? questionType : TEXT)
                .questionMediaUrl(mediaS3Key)           // STORE S3 KEY
                .questionMediaId(mediaFileId != null ? mediaFileId.toString() : null)
                .questionMediaType(mediaType)
                .questionThumbnailUrl(thumbnailS3Key)   // STORE S3 KEY
                .visibility(request.getVisibility() != null ? request.getVisibility() : QuestionVisibility.PRIVATE)
                .isUserCreated(true)
                .creator(creator)
                .isActive(true)
                .usageCount(0)
                .build();

        QuizQuestion saved = quizQuestionRepository.save(question);

        log.info("✅ Question created: ID={}, Type={}, MediaS3Key={}",
                saved.getId(), saved.getQuestionType(), mediaS3Key);

        // 6. Update media with question reference
        if (mediaFileId != null) {
            mediaStorageService.updateMediaEntityId(mediaFileId, saved.getId());
        }

        // 7. Convert to DTO and enrich with presigned URLs
        QuizQuestionDTO dto = QuizQuestionMapper.INSTANCE.toDTO(saved);
        return dtoEnricher.enrichWithUrls(dto);
    }


    private void validateMediaFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("File too large (max 100MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") ||
                contentType.startsWith("video/") ||
                contentType.startsWith("audio/"))) {
            throw new IllegalArgumentException("Unsupported media type");
        }
    }

    private QuestionType mapMediaTypeToQuestionType(MediaType mediaType) {
        return switch (mediaType) {
            case MediaType.IMAGE -> IMAGE;
            case MediaType.VIDEO -> VIDEO;
            case MediaType.DOCUMENT -> TEXT;
            case AUDIO -> null;
            case QUIZ_QUESTION -> null;
            case AVATAR -> null;
        };
    }

    /**
     * Create a user question with visibility policy
     */
    @Transactional
    public QuizQuestionDTO createUserQuestion(CreateQuizQuestionRequest request, Long creatorId) {
        log.info("Creating user question for creator: {} with visibility: {}",
                creatorId, request.getVisibility());

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + creatorId));

        // Validate QUIZ_ONLY visibility
        if (request.getVisibility() == QuestionVisibility.QUIZ_ONLY && request.getOriginalQuizId() == null) {
            throw new BadRequestException("Original quiz ID is required for QUIZ_ONLY visibility");
        }

        Topic topic = null;
        if (request.getTopic() != null && !request.getTopic().isEmpty()) {
            topic = topicService.findOrCreateTopic(request.getTopic(), creator);
        }

        QuizQuestion question = QuizQuestion.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : QuizDifficulty.MEDIUM)
                .topic(topic)
                .source(request.getSource())
                .additionalInfo(request.getAdditionalInfo())
                .isUserCreated(true)
                .creator(creator)
                .visibility(request.getVisibility())
                .isActive(true)
                .build();

        // Set original quiz if QUIZ_ONLY
        if (request.getVisibility() == QuestionVisibility.QUIZ_ONLY) {
            Challenge originalQuiz = challengeRepository.findById(request.getOriginalQuizId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + request.getOriginalQuizId()));
            question.setOriginalQuiz(originalQuiz);
        }

        question = quizQuestionRepository.save(question);
        log.info("Created user question with ID: {}", question.getId());

        return toDTO(question, creatorId);
    }

    /**
     * Get user's own questions with pagination
     */
    @Transactional(readOnly = true)
    public Page<QuizQuestionDTO> getUserQuestions(Long userId, Pageable pageable) {
        Page<QuizQuestion> questions = quizQuestionRepository.findByCreator_IdAndIsUserCreatedTrue(userId, pageable);
        return questions.map(q -> toDTO(q, userId));
    }

    /**
     * Search accessible questions for a user
     */
    @Transactional(readOnly = true)
    public Page<QuizQuestionDTO> searchAccessibleQuestions(Long userId, QuestionSearchRequest request) {
        List<Long> friendIds = relationshipService.getConnectedUserIds(userId);

        Page<QuizQuestion> questions = quizQuestionRepository.findAccessibleQuestions(
                userId,
                friendIds,
                request.getQuizId(),
                request.getPageable()
        );

        return questions.map(q -> toDTO(q, userId));
    }

    /**
     * Update question visibility
     */
    @Transactional
    public QuizQuestionDTO updateQuestionVisibility(Long questionId, Long userId, QuestionVisibility newVisibility, Long originalQuizId) {
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        // Only creator can change visibility
        if (!question.getCreator().getId().equals(userId)) {
            throw new AccessDeniedException("Only the creator can change question visibility");
        }

        // Validate QUIZ_ONLY visibility
        if (newVisibility == QuestionVisibility.QUIZ_ONLY && originalQuizId == null) {
            throw new BadRequestException("Original quiz ID is required for QUIZ_ONLY visibility");
        }

        question.setVisibility(newVisibility);

        if (newVisibility == QuestionVisibility.QUIZ_ONLY) {
            Challenge quiz = challengeRepository.findById(originalQuizId)
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + originalQuizId));
            question.setOriginalQuiz(quiz);
        } else {
            question.setOriginalQuiz(null);
        }

        question = quizQuestionRepository.save(question);
        return toDTO(question, userId);
    }

    /**
     * Convert QuizQuestion to DTO with access information
     */
    private QuizQuestionDTO toDTO(QuizQuestion question, Long currentUserId) {
        boolean isCreator = question.getCreator() != null &&
                question.getCreator().getId().equals(currentUserId);

        return QuizQuestionDTO.builder()
                // Basic identifiers
                .id(question.getId())
                .externalId(question.getExternalId())
                .legacyQuestionId(question.getLegacyQuestionId())

                // Core question content
                .question(question.getQuestion())
                .answer(question.getAnswer())

                // Classification
                .difficulty(question.getDifficulty())
                .questionType(question.getQuestionType())
                .topic(question.getTopic() != null ? question.getTopic().getName() : null)
                .source(question.getSource())

                // Enhanced metadata
                .authors(question.getAuthors())
                .comments(question.getComments())
                .passCriteria(question.getPassCriteria())
                .additionalInfo(question.getAdditionalInfo())

                // Media properties
                .questionMediaUrl(question.getQuestionMediaUrl())
                .questionMediaId(question.getQuestionMediaId())
                .questionMediaType(question.getQuestionMediaType())
                .questionThumbnailUrl(question.getQuestionThumbnailUrl())

                // User creation tracking
                .isUserCreated(question.getIsUserCreated())
                .creatorId(question.getCreator() != null ? question.getCreator().getId() : null)
                .creatorUsername(question.getCreator() != null ? question.getCreator().getUsername() : null)

                // Access control
                .visibility(question.getVisibility())
                .originalQuizId(question.getOriginalQuiz() != null ? question.getOriginalQuiz().getId() : null)
                .originalQuizTitle(question.getOriginalQuiz() != null ? question.getOriginalQuiz().getTitle() : null)

                // Access information for current user
                .canEdit(isCreator)
                .canDelete(isCreator)
                .canUseInQuiz(accessService.canAccessQuestion(question, currentUserId))

                // Status and usage
                .isActive(question.getIsActive())
                .usageCount(question.getUsageCount())

                // Timestamps
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    // =============================================================================
    // QUESTION MANAGEMENT METHODS
    // =============================================================================


    public List<QuizQuestionDTO> getUserQuestions(Long userId) {
        log.info("Getting user questions for user: {}", userId);

        List<QuizQuestion> questions = quizQuestionRepository
                .findByCreator_IdAndIsUserCreatedTrueOrderByCreatedAtDesc(userId);

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

        String normalizedKeyword = keyword != null && !keyword.trim().isEmpty()
                ? keyword.toLowerCase().trim()
                : null;

        List<QuizQuestion> questions = quizQuestionRepository
                .searchByKeyword(normalizedKeyword, PageRequest.of(0, limit));

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
                .userId(hostUserId)
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
            long durationSeconds = Duration
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

    public List<QuizSessionDTO> getSessionsByExactQuestionSource(Long creatorId, QuestionSource questionSource) {
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

//        if ("user".equals(request.getQuestionSource()) && request.getCustomQuestionIds() != null) {
            // Use user-specified questions
            questions = quizQuestionRepository.findAllById(request.getCustomQuestionIds());
            if (questions.size() < request.getTotalRounds()) {
                throw new IllegalArgumentException("Not enough custom questions selected");
            }
//        } else {
//            // Use random questions by difficulty
//            questions = quizQuestionRepository.findByDifficultyOrderByUsageCountAsc(
//                    request.getDifficulty(), PageRequest.of(0, request.getTotalRounds()));
//            if (questions.size() < request.getTotalRounds()) {
//                throw new IllegalStateException("Not enough questions available for the selected difficulty");
//            }
//        }

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

    protected QuizQuestionDTO convertQuestionToDTO(QuizQuestion question) {
        return QuizQuestionDTO.builder()
                .id(question.getId())
                .question(question.getQuestion())
                .answer(question.getAnswer())
                .difficulty(question.getDifficulty())
                .topic(question.getTopic()!=null ? question.getTopic().getName(): "")
                .source(question.getSource())
                .additionalInfo(question.getAdditionalInfo())
                .isUserCreated(question.getIsUserCreated())
                .creatorId(question.getCreator() != null ? question.getCreator().getId() : null)
                .externalId(question.getExternalId())
                .usageCount(question.getUsageCount())
                .createdAt(question.getCreatedAt())
//                .lastUsed(question.getLastUsed())
                .build();
    }

    QuizSessionDTO convertSessionToDTO(QuizSession session) {
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