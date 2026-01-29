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
import com.my.challenger.service.ExternalMediaValidator;
import com.my.challenger.service.WWWGameService;
import com.my.challenger.util.YouTubeUrlParser;
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

import com.my.challenger.entity.enums.QuestionType;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizRoundRepository quizRoundRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final WWWGameService gameService;
    private final TopicService topicService;
    private final QuestionAccessService accessService;
    private final UserRelationshipService relationshipService;
    private final MinioMediaStorageService mediaStorageService;
    private final QuizQuestionDTOEnricher dtoEnricher;
    private final ExternalMediaValidator externalMediaValidator;

    @Transactional
    public QuizQuestionDTO createQuestionWithMedia(
            CreateQuizQuestionRequest request,
            MultipartFile mediaFile,
            Long userId) {

        log.info("========== QuestionService.createQuestionWithMedia START ==========");
        log.info("📥 Request questionType: {}", request.getQuestionType());
        log.info("📎 mediaFile is null: {}", mediaFile == null);
        log.info("📎 mediaFile isEmpty: {}", mediaFile != null ? mediaFile.isEmpty() : "N/A");

        // 1. Get user
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

        // Validate external media if present
        externalMediaValidator.validate(request);

        // 2. Handle media upload
        Long mediaFileId = null;
        String mediaS3Key = null;
        String thumbnailS3Key = null;
        MediaType mediaType = null;

        if (mediaFile != null && !mediaFile.isEmpty()) {
            log.info("📤 Processing media file: {} ({} bytes, type: {})",
                    mediaFile.getOriginalFilename(),
                    mediaFile.getSize(),
                    mediaFile.getContentType());

            validateMediaFile(mediaFile);
            MediaFile storedMedia = mediaStorageService.storeMedia(
                    mediaFile, null, MediaCategory.QUIZ_QUESTION, userId);

            mediaFileId = storedMedia.getId();
            mediaS3Key = storedMedia.getS3Key();
            thumbnailS3Key = storedMedia.getThumbnailPath();
            mediaType = storedMedia.getMediaType();

            log.info("✅ Media stored: ID={}, S3Key={}, Type={}", mediaFileId, mediaS3Key, mediaType);
        } else {
            log.warn("⚠️ No media file to process (null: {}, empty: {})",
                    mediaFile == null,
                    mediaFile != null ? mediaFile.isEmpty() : "N/A");
        }

        // 3. Get/create topic
        Topic topic = null;
        if (request.getTopic() != null && !request.getTopic().isBlank()) {
            topic = topicService.getOrCreateTopic(request.getTopic());
        }

        // 4. Auto-detect question type from media
        QuestionType questionType = request.getQuestionType();
        log.info("📄 Request questionType: {}, mediaType: {}", questionType, mediaType);

        if ((questionType == null || questionType == QuestionType.TEXT) && mediaType != null) {
            questionType = mapMediaTypeToQuestionType(mediaType);
            log.info("🔄 Auto-detected questionType from media: {}", questionType);
        } else if (request.getMediaSourceType() != null && request.getMediaSourceType() != MediaSourceType.UPLOADED) {
            // Auto-detect for external media if not set
             if (questionType == null || questionType == QuestionType.TEXT) {
                 // Default to VIDEO for YouTube/Vimeo, AUDIO for SoundCloud?
                 // For now, let's assume VIDEO if external URL is present
                 questionType = QuestionType.VIDEO; 
             }
        }

        // 5. Build and save question
        QuizQuestion question = QuizQuestion.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : QuizDifficulty.MEDIUM)
                .topic(topic)
                .questionType(questionType != null ? questionType : QuestionType.TEXT)
                .questionMediaUrl(mediaS3Key)
                .questionMediaId(mediaFileId != null ? mediaFileId : null)
                .questionMediaType(mediaType)
                .questionThumbnailUrl(thumbnailS3Key)
                .visibility(request.getVisibility() != null ? request.getVisibility() : QuestionVisibility.PRIVATE)
                .isUserCreated(true)
                .creator(creator)
                .isActive(true)
                .usageCount(0)
                // External Media Fields
                .mediaSourceType(request.getMediaSourceType() != null ? request.getMediaSourceType() : MediaSourceType.UPLOADED)
                .externalMediaUrl(request.getExternalMediaUrl())
                .externalMediaId(extractExternalId(request))
                .questionVideoStartTime(request.getQuestionVideoStartTime())
                .questionVideoEndTime(request.getQuestionVideoEndTime())
                .answerMediaUrl(request.getAnswerMediaUrl())
                .answerExternalMediaId(extractAnswerExternalId(request))
                .answerVideoStartTime(request.getAnswerVideoStartTime())
                .answerVideoEndTime(request.getAnswerVideoEndTime())
                .answerTextVerification(request.getAnswerTextVerification())
                .build();

        QuizQuestion saved = quizQuestionRepository.save(question);

        log.info("✅ Question saved: ID={}, Type={}, MediaS3Key={}, MediaId={}",
                saved.getId(), saved.getQuestionType(), mediaS3Key, mediaFileId);
        log.info("========== QuestionService.createQuestionWithMedia END ==========");

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
            case IMAGE -> QuestionType.IMAGE;
            case VIDEO -> QuestionType.VIDEO;
            case AUDIO -> QuestionType.AUDIO;
            case DOCUMENT -> QuestionType.TEXT;
            case QUIZ_QUESTION, AVATAR -> null;
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

        // Validate external media
        externalMediaValidator.validate(request);

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
                // External Media Fields
                .mediaSourceType(request.getMediaSourceType() != null ? request.getMediaSourceType() : MediaSourceType.UPLOADED)
                .externalMediaUrl(request.getExternalMediaUrl())
                .externalMediaId(extractExternalId(request))
                .questionVideoStartTime(request.getQuestionVideoStartTime())
                .questionVideoEndTime(request.getQuestionVideoEndTime())
                .answerMediaUrl(request.getAnswerMediaUrl())
                .answerExternalMediaId(extractAnswerExternalId(request))
                .answerVideoStartTime(request.getAnswerVideoStartTime())
                .answerVideoEndTime(request.getAnswerVideoEndTime())
                .answerTextVerification(request.getAnswerTextVerification())
                .build();

        // Set original quiz if QUIZ_ONLY
        if (request.getVisibility() == QuestionVisibility.QUIZ_ONLY) {
            Challenge originalQuiz = challengeRepository.findById(request.getOriginalQuizId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + request.getOriginalQuizId()));
            question.setOriginalQuiz(originalQuiz);
        }

        question = quizQuestionRepository.save(question);
        log.info("Created user question with ID: {}", question.getId());

        return dtoEnricher.enrichWithUrls(convertQuestionToDTO(question));
    }

    /**
     * Get user's own questions with pagination
     */
    @Transactional(readOnly = true)
    public Page<QuizQuestionDTO> getUserQuestions(Long userId, Pageable pageable) {
        Page<QuizQuestion> questions = quizQuestionRepository.findByCreator_IdAndIsUserCreatedTrue(userId, pageable);
        return questions.map(q -> dtoEnricher.enrichWithUrls(convertQuestionToDTO(q)));
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

        return questions.map(q -> dtoEnricher.enrichWithUrls(convertQuestionToDTO(q)));
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
        return dtoEnricher.enrichWithUrls(convertQuestionToDTO(question));
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
            log.info("Session {} is already completed, returning existing session", sessionId);
            return convertSessionToDTO(session);
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
        log.info("Creating quiz rounds for session: {} using source: {}", session.getId(), request.getQuestionSource());

        List<QuizQuestion> questions;

        if (QuestionSource.user.equals(request.getQuestionSource()) && request.getCustomQuestionIds() != null && !request.getCustomQuestionIds().isEmpty()) {
            log.info("Using {} custom questions for session {}", request.getCustomQuestionIds().size(), session.getId());
            questions = quizQuestionRepository.findAllById(request.getCustomQuestionIds());
            if (questions.size() < request.getTotalRounds()) {
                log.error("Insufficient custom questions: provided {}, required {}", questions.size(), request.getTotalRounds());
                throw new IllegalArgumentException("Not enough custom questions selected. Selected: " + questions.size() + ", Required: " + request.getTotalRounds());
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
                    .build();
            quizRoundRepository.save(round);

            // Increment usage count for the question
            QuizQuestion question = questions.get(i);
            question.setUsageCount(question.getUsageCount() + 1);
            quizQuestionRepository.save(question);
        }
        log.info("Successfully created {} rounds for session {}", roundsToCreate, session.getId());
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
        log.debug("Converting question {} - questionType={}, audioChallengeType={}",
                question.getId(), question.getQuestionType(), question.getAudioChallengeType());
        return QuizQuestionMapper.INSTANCE.toDTO(question);
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
   
    private String extractExternalId(CreateQuizQuestionRequest request) {
        if (request.getMediaSourceType() == MediaSourceType.YOUTUBE && request.getExternalMediaUrl() != null) {
            return YouTubeUrlParser.extractVideoId(request.getExternalMediaUrl()).orElse(null);
        }
        return null;
    }

    private String extractAnswerExternalId(CreateQuizQuestionRequest request) {
        if (request.getAnswerMediaUrl() != null && YouTubeUrlParser.isYouTubeUrl(request.getAnswerMediaUrl())) {
            return YouTubeUrlParser.extractVideoId(request.getAnswerMediaUrl()).orElse(null);
        }
        return null;
    }
}
