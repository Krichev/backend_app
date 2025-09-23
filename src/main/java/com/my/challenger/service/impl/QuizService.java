// src/main/java/com/my/challenger/service/impl/QuizService.java - Enhanced for Multimedia
package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuizSessionStatus;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.QuizRound;
import com.my.challenger.entity.quiz.QuizSession;
import com.my.challenger.repository.*;
import com.my.challenger.service.WWWGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    protected final QuizQuestionRepository quizQuestionRepository;
    protected final QuizSessionRepository quizSessionRepository;
    protected final QuizRoundRepository quizRoundRepository;
    protected final ChallengeRepository challengeRepository;
    protected final UserRepository userRepository;
    protected final MediaFileRepository mediaFileRepository;
    protected final WWWGameService gameService;
    protected final MediaStorageService mediaStorageService;

    // =============================================================================
    // MULTIMEDIA QUESTION MANAGEMENT METHODS
    // =============================================================================

    @Transactional
    public QuizQuestionDTO createUserQuestion(CreateQuizQuestionRequest request, Long creatorId) {
        log.info("Creating user question for creator: {} with type: {}", creatorId, request.getQuestionType());

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

        QuizQuestion.QuizQuestionBuilder questionBuilder = QuizQuestion.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .difficulty(request.getDifficulty())
                .topic(request.getTopic())
                .source(request.getSource())
                .additionalInfo(request.getAdditionalInfo())
                .questionType(request.getQuestionType() != null ? request.getQuestionType() : QuestionType.TEXT)
                .isUserCreated(true)
                .creator(creator)
                .usageCount(0);

        // Handle media if provided
        if (request.getQuestionMediaId() != null) {
            Optional<MediaFile> mediaFile = mediaFileRepository.findById(Long.parseLong(request.getQuestionMediaId()));
            if (mediaFile.isPresent()) {
                questionBuilder
                        .questionMediaId(request.getQuestionMediaId())
                        .questionMediaUrl(mediaStorageService.getMediaUrl(mediaFile.get()))
                        .questionMediaType(mediaFile.get().getContentType())
                        .questionThumbnailUrl(mediaFile.get().getThumbnailPath());

                log.info("Associated media file {} with question", request.getQuestionMediaId());
            }
        } else if (request.getQuestionMediaUrl() != null) {
            questionBuilder
                    .questionMediaUrl(request.getQuestionMediaUrl())
                    .questionMediaType(inferMediaTypeFromUrl(request.getQuestionMediaUrl()));
        }

        QuizQuestion saved = quizQuestionRepository.save(questionBuilder.build());
        log.info("Created multimedia question with ID: {} and type: {}", saved.getId(), saved.getQuestionType());

        return convertQuestionToDTO(saved);
    }

    @Transactional
    public QuizQuestionDTO updateQuestionMedia(Long questionId, String mediaId, Long userId) {
        log.info("Updating media for question {} with media {}", questionId, mediaId);

        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        // Verify ownership
        if (question.getIsUserCreated() && !question.getCreator().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only update your own questions");
        }

        // Get media file
        MediaFile mediaFile = mediaFileRepository.findById(Long.parseLong(mediaId))
                .orElseThrow(() -> new IllegalArgumentException("Media file not found"));

        // Update question with media
        question.setQuestionMediaId(mediaId);
        question.setQuestionMediaUrl(mediaStorageService.getMediaUrl(mediaFile));
        question.setQuestionMediaType(mediaFile.getContentType());
        question.setQuestionThumbnailUrl(mediaFile.getThumbnailPath());

        // Update question type based on media
        question.setQuestionType(inferQuestionTypeFromMedia(mediaFile.getContentType()));

        QuizQuestion updated = quizQuestionRepository.save(question);
        log.info("Successfully updated question {} with media", questionId);

        return convertQuestionToDTO(updated);
    }

    @Transactional
    public boolean removeQuestionMedia(Long questionId, Long userId) {
        log.info("Removing media from question {} by user {}", questionId, userId);

        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        // Verify ownership
        if (question.getIsUserCreated() && !question.getCreator().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only modify your own questions");
        }

        // Store old media ID for cleanup
        String oldMediaId = question.getQuestionMediaId();

        // Remove media references
        question.setQuestionMediaId(null);
        question.setQuestionMediaUrl(null);
        question.setQuestionMediaType(null);
        question.setQuestionThumbnailUrl(null);
        question.setQuestionType(QuestionType.TEXT);

        quizQuestionRepository.save(question);

        // Optionally delete the media file if not used elsewhere
        if (oldMediaId != null) {
            cleanupUnusedMediaFile(Long.parseLong(oldMediaId));
        }

        log.info("Successfully removed media from question {}", questionId);
        return true;
    }

    // =============================================================================
    // ENHANCED SESSION CREATION WITH MULTIMEDIA SUPPORT
    // =============================================================================

    @Transactional
    public QuizSessionResponseDTO startQuizSession(StartQuizSessionRequest request) {
        log.info("Starting multimedia quiz session for challenge: {}", request.getChallengeId());

        // Validate challenge
        Challenge challenge = challengeRepository.findById(Long.parseLong(request.getChallengeId()))
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        User hostUser = userRepository.findById(challenge.getCreatedBy())
                .orElseThrow(() -> new IllegalArgumentException("Host user not found"));

        // Create quiz session
        QuizSession session = QuizSession.builder()
                .challenge(challenge)
                .hostUser(hostUser)
                .teamName(request.getTeamName())
                .teamMembers(String.join(",", request.getTeamMembers()))
                .difficulty(request.getDifficulty())
                .roundTimeSeconds(request.getRoundTimeSeconds())
                .totalRounds(request.getTotalRounds())
                .currentRound(0)
                .status(QuizSessionStatus.WAITING)
                .enableAiHost(request.isEnableAiHost())
                .questionSource(request.getQuestionSource())
                .build();

        session = quizSessionRepository.save(session);
        log.info("Created quiz session with ID: {}", session.getId());

        // Create rounds with multimedia support
        createMultimediaQuizRounds(session, request);

        // Update session status
        session.setStatus(QuizSessionStatus.IN_PROGRESS);
        session = quizSessionRepository.save(session);

        return QuizSessionResponseDTO.builder()
                .sessionId(session.getId().toString())
                .challengeId(request.getChallengeId())
                .teamName(request.getTeamName())
                .status(session.getStatus().name())
                .totalRounds(session.getTotalRounds())
                .hasMultimedia(hasMultimediaQuestions(session.getId()))
                .build();
    }

    @Transactional
    protected void createMultimediaQuizRounds(QuizSession session, StartQuizSessionRequest request) {
        log.info("Creating multimedia quiz rounds for session {} with source: {}",
                session.getId(), request.getQuestionSource());

        List<QuizQuestion> questionsToUse = new ArrayList<>();

        switch (request.getQuestionSource()) {
            case "app":
                questionsToUse = handleAppQuestions(request, session.getHostUser());
                break;
            case "user":
                questionsToUse = handleUserQuestions(request, session.getHostUser());
                break;
            default:
                throw new IllegalArgumentException("Invalid question source: " + request.getQuestionSource());
        }

        // Create rounds from the selected/created questions
        createRoundsFromQuestions(session, questionsToUse);

        log.info("Successfully created {} multimedia rounds for session {}",
                questionsToUse.size(), session.getId());
    }

    private List<QuizQuestion> handleAppQuestions(StartQuizSessionRequest request, User hostUser) {
        List<QuizQuestion> questions = new ArrayList<>();

        if (request.getAppQuestions() != null && !request.getAppQuestions().isEmpty()) {
            log.info("Saving {} app-generated questions to database", request.getAppQuestions().size());

            for (AppQuestionData appQuestion : request.getAppQuestions()) {
                QuizQuestion.QuizQuestionBuilder questionBuilder = QuizQuestion.builder()
                        .question(appQuestion.getQuestion())
                        .answer(appQuestion.getAnswer())
                        .difficulty(appQuestion.getDifficulty())
                        .topic(appQuestion.getTopic())
                        .additionalInfo(appQuestion.getAdditionalInfo())
                        .creator(hostUser)
                        .isUserCreated(false)
                        .externalId(appQuestion.getExternalId())
                        .source(appQuestion.getSource() != null ? appQuestion.getSource() : "APP_GENERATED")
                        .questionType(appQuestion.getQuestionType() != null ? appQuestion.getQuestionType() : QuestionType.TEXT);

                // Handle multimedia for app questions
                if (appQuestion.getQuestionMediaUrl() != null) {
                    questionBuilder
                            .questionMediaUrl(appQuestion.getQuestionMediaUrl())
                            .questionMediaType(inferMediaTypeFromUrl(appQuestion.getQuestionMediaUrl()));

                    if (appQuestion.getQuestionMediaId() != null) {
                        questionBuilder.questionMediaId(appQuestion.getQuestionMediaId());
                    }
                }

                QuizQuestion savedQuestion = quizQuestionRepository.save(questionBuilder.build());
                questions.add(savedQuestion);

                log.debug("Created multimedia app question: {} (ID: {}, Type: {})",
                        savedQuestion.getQuestion(), savedQuestion.getId(), savedQuestion.getQuestionType());
            }
        }

        if (questions.isEmpty()) {
            throw new IllegalArgumentException("No app questions provided");
        }

        return questions;
    }

    private List<QuizQuestion> handleUserQuestions(StartQuizSessionRequest request, User hostUser) {
        List<QuizQuestion> questions = new ArrayList<>();

        // Add existing user questions
        if (request.getCustomQuestionIds() != null && !request.getCustomQuestionIds().isEmpty()) {
            List<QuizQuestion> existingQuestions = quizQuestionRepository.findAllById(request.getCustomQuestionIds());
            questions.addAll(existingQuestions);

            log.info("Added {} existing user questions", existingQuestions.size());
        }

        // Create new custom questions with multimedia support
        if (request.getNewCustomQuestions() != null && !request.getNewCustomQuestions().isEmpty()) {
            log.info("Creating {} new custom questions", request.getNewCustomQuestions().size());

            for (CreateQuestionRequest newQuestion : request.getNewCustomQuestions()) {
                QuizQuestion.QuizQuestionBuilder questionBuilder = QuizQuestion.builder()
                        .question(newQuestion.getQuestion())
                        .answer(newQuestion.getAnswer())
                        .difficulty(newQuestion.getDifficulty())
                        .topic(newQuestion.getTopic())
                        .additionalInfo(newQuestion.getAdditionalInfo())
                        .creator(hostUser)
                        .isUserCreated(true)
                        .source("USER_CREATED")
                        .questionType(newQuestion.getQuestionType() != null ? newQuestion.getQuestionType() : QuestionType.TEXT);

                // Handle multimedia for new custom questions
                if (newQuestion.getQuestionMediaUrl() != null) {
                    questionBuilder
                            .questionMediaUrl(newQuestion.getQuestionMediaUrl())
                            .questionMediaType(inferMediaTypeFromUrl(newQuestion.getQuestionMediaUrl()));

                    if (newQuestion.getQuestionMediaId() != null) {
                        questionBuilder.questionMediaId(newQuestion.getQuestionMediaId());
                    }
                }

                QuizQuestion savedQuestion = quizQuestionRepository.save(questionBuilder.build());
                questions.add(savedQuestion);

                log.debug("Created new multimedia user question: {} (ID: {}, Type: {})",
                        savedQuestion.getQuestion(), savedQuestion.getId(), savedQuestion.getQuestionType());
            }
        }

        if (questions.isEmpty()) {
            throw new IllegalArgumentException("No questions provided for user question source");
        }

        return questions;
    }

    private void createRoundsFromQuestions(QuizSession session, List<QuizQuestion> questions) {
        for (int i = 0; i < questions.size() && i < session.getTotalRounds(); i++) {
            QuizQuestion question = questions.get(i);

            QuizRound round = QuizRound.builder()
                    .quizSession(session)
                    .question(question)
                    .roundNumber(i + 1)
                    .isCorrect(false)
                    .hintUsed(false)
                    .voiceRecordingUsed(false)
                    .mediaInteractionCount(0)
                    .build();

            quizRoundRepository.save(round);
        }
    }

    // =============================================================================
    // MULTIMEDIA UTILITY METHODS
    // =============================================================================

    private QuestionType inferQuestionTypeFromMedia(String mimeType) {
        if (mimeType == null) return QuestionType.TEXT;

        if (mimeType.startsWith("video/")) return QuestionType.VIDEO;
        if (mimeType.startsWith("audio/")) return QuestionType.AUDIO;
        if (mimeType.startsWith("image/")) return QuestionType.IMAGE;

        return QuestionType.TEXT;
    }

    private String inferMediaTypeFromUrl(String url) {
        if (url == null) return null;

        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".mp4") || lowerUrl.contains(".mov") || lowerUrl.contains(".avi")) {
            return "video/mp4";
        }
        if (lowerUrl.contains(".mp3") || lowerUrl.contains(".wav") || lowerUrl.contains(".m4a")) {
            return "audio/mpeg";
        }
        if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") || lowerUrl.contains(".png")) {
            return "image/jpeg";
        }

        return "application/octet-stream";
    }

    private boolean hasMultimediaQuestions(Long sessionId) {
        return quizRoundRepository.existsByQuizSessionIdAndQuestionQuestionTypeNot(sessionId, QuestionType.TEXT);
    }

    private void cleanupUnusedMediaFile(Long mediaId) {
        try {
            // Check if media is used by other questions
            boolean isUsed = quizQuestionRepository.existsByQuestionMediaId(mediaId.toString());

            if (!isUsed) {
                mediaStorageService.deleteMedia(mediaId); // System cleanup
                log.info("Cleaned up unused media file: {}", mediaId);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup media file {}: {}", mediaId, e.getMessage());
        }
    }

    // =============================================================================
    // ENHANCED DTO CONVERSION WITH MULTIMEDIA SUPPORT
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
                .creatorName(question.getCreator() != null ? question.getCreator().getUsername() : null)
                .createdAt(question.getCreatedAt())
                .usageCount(question.getUsageCount())
                .questionType(question.getQuestionType())
                .questionMediaUrl(question.getQuestionMediaUrl())
                .questionMediaId(question.getQuestionMediaId())
                .questionMediaType(question.getQuestionMediaType())
                .questionThumbnailUrl(question.getQuestionThumbnailUrl())
                .build();
    }

    // =============================================================================
    // QUERY METHODS FOR MULTIMEDIA QUESTIONS
    // =============================================================================

    public List<QuizQuestionDTO> getMultimediaQuestionsByType(QuestionType questionType, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        return quizQuestionRepository.findByQuestionTypeOrderByCreatedAtDesc(questionType, pageRequest)
                .stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    public List<QuizQuestionDTO> getUserMultimediaQuestions(Long userId) {
        return quizQuestionRepository.findByCreatorIdAndQuestionTypeNotOrderByCreatedAtDesc(userId, QuestionType.TEXT)
                .stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    public MediaUsageStatsDTO getMediaUsageStats(Long userId) {
        // This would require additional repository methods
        // Placeholder implementation
        return MediaUsageStatsDTO.builder()
                .totalMediaQuestions(0L)
                .videoQuestions(0L)
                .audioQuestions(0L)
                .imageQuestions(0L)
                .totalStorageUsedMB(0.0)
                .build();
    }

    // =============================================================================
    // EXISTING METHODS (UNCHANGED)
    // =============================================================================

    public List<QuizQuestionDTO> getUserQuestions(Long userId) {
        List<QuizQuestion> questions = quizQuestionRepository.findByCreatorIdOrderByCreatedAtDesc(userId);
        return questions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    public Optional<QuizQuestionDTO> getQuestionById(Long questionId) {
        return quizQuestionRepository.findById(questionId)
                .map(this::convertQuestionToDTO);
    }

    public boolean deleteUserQuestion(Long questionId, Long userId) {
        Optional<QuizQuestion> questionOpt = quizQuestionRepository.findById(questionId);

        if (questionOpt.isEmpty()) {
            return false;
        }

        QuizQuestion question = questionOpt.get();

        if (!question.getIsUserCreated() || !question.getCreator().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own questions");
        }

        // Cleanup media if exists
        if (question.getQuestionMediaId() != null) {
            cleanupUnusedMediaFile(Long.parseLong(question.getQuestionMediaId()));
        }

        quizQuestionRepository.delete(question);
        return true;
    }
}

// Additional DTO classes for multimedia support

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class MediaUsageStatsDTO {
    private Long totalMediaQuestions;
    private Long videoQuestions;
    private Long audioQuestions;
    private Long imageQuestions;
    private Double totalStorageUsedMB;
}

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class QuizSessionResponseDTO {
    private String sessionId;
    private String challengeId;
    private String teamName;
    private String status;
    private Integer totalRounds;
    private Boolean hasMultimedia;
}