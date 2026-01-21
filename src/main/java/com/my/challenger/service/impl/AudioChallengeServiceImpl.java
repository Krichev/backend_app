package com.my.challenger.service.impl;

import com.my.challenger.dto.audio.AudioChallengeConfigDTO;
import com.my.challenger.dto.audio.AudioChallengeSubmissionDTO;
import com.my.challenger.dto.audio.CreateAudioQuestionRequest;
import com.my.challenger.dto.audio.QuestionResponseDTO;
import com.my.challenger.entity.AudioChallengeSubmission;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.AudioChallengeType;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.Topic;
import com.my.challenger.exception.InvalidAudioSegmentException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.AudioChallengeSubmissionRepository;
import com.my.challenger.repository.MediaFileRepository;
import com.my.challenger.repository.QuizQuestionRepository;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.AudioChallengeService;
import com.my.challenger.service.integration.KaraokeServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioChallengeServiceImpl implements AudioChallengeService {

    private final QuizQuestionRepository questionRepository;
    private final AudioChallengeSubmissionRepository submissionRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final TopicService topicService;
    private final MinioMediaStorageService mediaStorageService;
    private final KaraokeServiceClient karaokeClient;

    @Override
    @Transactional
    public QuestionResponseDTO createAudioQuestion(
            CreateAudioQuestionRequest request,
            MultipartFile referenceAudio,
            Long creatorId) {

        log.info("🎵 Creating audio challenge question: type={}, creator={}",
                request.getAudioChallengeType(), creatorId);

        // 1. Validate creator
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found"));

        // 2. Validate audio challenge type requirements
        AudioChallengeType challengeType = request.getAudioChallengeType();
        if (challengeType.requiresReferenceAudio() &&
            (referenceAudio == null || referenceAudio.isEmpty())) {
            throw new InvalidAudioSegmentException(
                    "Reference audio is required for " + challengeType + " challenges");
        }

        // 3. Upload reference audio if provided
        MediaFile audioMedia = null;
        if (referenceAudio != null && !referenceAudio.isEmpty()) {
            validateAudioFile(referenceAudio);
            audioMedia = mediaStorageService.storeMedia(
                    referenceAudio, null, MediaCategory.QUIZ_QUESTION, creatorId);
            log.info("✅ Reference audio uploaded: id={}, path={}",
                    audioMedia.getId(), audioMedia.getS3Key());
        }

        // 4. Get or create topic
        Topic topic = null;
        if (request.getTopic() != null && !request.getTopic().isBlank()) {
            topic = topicService.getOrCreateTopic(request.getTopic());
        }

        // Generate default answer for audio challenges if not provided
        String answer = request.getAnswer();
        if (answer == null || answer.isBlank()) {
            answer = generateDefaultAudioAnswer(challengeType, request);
        }

        // 5. Build question entity
        QuizQuestion question = QuizQuestion.builder()
                .question(request.getQuestion())
                .answer(answer)
                .questionType(QuestionType.AUDIO)
                .audioChallengeType(challengeType)
                .audioReferenceMedia(audioMedia)
                .questionMediaUrl(audioMedia != null ? audioMedia.getS3Key() : null)
                .questionMediaId(audioMedia != null ? audioMedia.getId() : null)
                .questionMediaType(MediaType.AUDIO)
                .audioSegmentStart(request.getAudioSegmentStart() != null ?
                        request.getAudioSegmentStart() : 0.0)
                .audioSegmentEnd(request.getAudioSegmentEnd())
                .minimumScorePercentage(request.getMinimumScorePercentage() != null ?
                        request.getMinimumScorePercentage() : 60)
                .rhythmBpm(request.getRhythmBpm())
                .rhythmTimeSignature(request.getRhythmTimeSignature())
                .difficulty(request.getDifficulty())
                .visibility(request.getVisibility())
                .topic(topic)
                .additionalInfo(request.getAdditionalInfo())
                .creator(creator)
                .isUserCreated(true)
                .build();

        // 6. Save question
        QuizQuestion savedQuestion = questionRepository.save(question);
        log.info("✅ Audio challenge question created: id={}", savedQuestion.getId());

        // 7. Return response DTO
        return mapToResponseDTO(savedQuestion);
    }

    @Override
    @Transactional
    public QuestionResponseDTO updateAudioConfig(Long questionId, AudioChallengeConfigDTO config) {
        log.info("🔧 Updating audio config for question: {}", questionId);

        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        // Update audio challenge type
        if (config.getAudioChallengeType() != null) {
            question.setAudioChallengeType(config.getAudioChallengeType());
        }

        // Update reference audio
        if (config.getAudioReferenceMediaId() != null) {
            MediaFile audioMedia = mediaFileRepository.findById(config.getAudioReferenceMediaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Audio media not found: " + config.getAudioReferenceMediaId()));

            if (audioMedia.getMediaType() != MediaType.AUDIO) {
                throw new InvalidAudioSegmentException(
                        "Media must be AUDIO type, but was: " + audioMedia.getMediaType());
            }

            question.setAudioReferenceMedia(audioMedia);
            question.setQuestionMediaUrl(audioMedia.getS3Key());
            question.setQuestionMediaId(audioMedia.getId());
        }

        // Update segment times
        if (config.getAudioSegmentStart() != null) {
            question.setAudioSegmentStart(config.getAudioSegmentStart());
        }
        question.setAudioSegmentEnd(config.getAudioSegmentEnd());

        // Update scoring config
        if (config.getMinimumScorePercentage() != null) {
            question.setMinimumScorePercentage(config.getMinimumScorePercentage());
        }

        // Update rhythm config
        if (config.getRhythmBpm() != null) {
            question.setRhythmBpm(config.getRhythmBpm());
        }
        if (config.getRhythmTimeSignature() != null) {
            question.setRhythmTimeSignature(config.getRhythmTimeSignature());
        }

        QuizQuestion saved = questionRepository.save(question);
        log.info("✅ Audio config updated for question: {}", questionId);

        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public AudioChallengeSubmissionDTO submitRecording(
            Long questionId,
            Long userId,
            MultipartFile audioFile) {

        log.info("🎤 Receiving audio submission: question={}, user={}", questionId, userId);

        // 1. Validate question
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        if (!question.isAudioChallenge()) {
            throw new IllegalArgumentException("Question is not an audio challenge");
        }

        // 2. Validate and store submission audio
        validateAudioFile(audioFile);
        MediaFile submissionMedia = mediaStorageService.storeMedia(
                audioFile, null, MediaCategory.CHALLENGE_PROOF, userId);

        // 3. Create submission record
        AudioChallengeSubmission submission = AudioChallengeSubmission.builder()
                .question(question)
                .userId(userId)
                .submissionAudioPath(submissionMedia.getFilePath())
                .processingStatus("PENDING")
                .processingProgress(0)
                .build();

        submission = submissionRepository.save(submission);
        log.info("✅ Submission created: id={}", submission.getId());

        // 4. Trigger async processing
        processSubmissionAsync(submission.getId());

        return mapToSubmissionDTO(submission);
    }

    @Async
    protected void processSubmissionAsync(Long submissionId) {
        try {
            processSubmission(submissionId);
        } catch (Exception e) {
            log.error("❌ Error processing submission {}: {}", submissionId, e.getMessage(), e);
            markSubmissionFailed(submissionId, e.getMessage());
        }
    }

    @Transactional
    protected void processSubmission(Long submissionId) {
        log.info("⚙️ Processing submission: {}", submissionId);

        AudioChallengeSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        submission.setProcessingStatus("PROCESSING");
        submission.setProcessingProgress(10);
        submissionRepository.save(submission);

        QuizQuestion question = submission.getQuestion();
        AudioChallengeType challengeType = question.getAudioChallengeType();

        // Get scoring weights based on challenge type
        double[] weights = challengeType.getScoringWeights();

        // Call Karaoke service for scoring
        var scoringResult = karaokeClient.scoreAudio(
                submission.getSubmissionAudioPath(),
                question.getAudioReferenceMedia() != null ?
                        question.getAudioReferenceMedia().getFilePath() : null,
                challengeType,
                question.getRhythmBpm(),
                question.getRhythmTimeSignature()
        );

        submission.setProcessingProgress(80);
        submissionRepository.save(submission);

        // Calculate weighted overall score
        double pitchScore = scoringResult.getPitchScore() != null ? scoringResult.getPitchScore() : 0;
        double rhythmScore = scoringResult.getRhythmScore() != null ? scoringResult.getRhythmScore() : 0;
        double voiceScore = scoringResult.getVoiceScore() != null ? scoringResult.getVoiceScore() : 0;

        double overallScore = (pitchScore * weights[0]) +
                             (rhythmScore * weights[1]) +
                             (voiceScore * weights[2]);

        // Update submission with results
        submission.setPitchScore(pitchScore);
        submission.setRhythmScore(rhythmScore);
        submission.setVoiceScore(voiceScore);
        submission.setOverallScore(overallScore);
        submission.setDetailedMetrics(scoringResult.getDetailedMetrics());
        submission.setProcessingStatus("COMPLETED");
        submission.setProcessingProgress(100);
        submission.setProcessedAt(LocalDateTime.now());

        submissionRepository.save(submission);
        log.info("✅ Submission processed: id={}, score={}", submissionId, overallScore);
    }

    @Transactional
    protected void markSubmissionFailed(Long submissionId, String errorMessage) {
        submissionRepository.findById(submissionId).ifPresent(submission -> {
            submission.setProcessingStatus("FAILED");
            submission.setProcessedAt(LocalDateTime.now());
            submissionRepository.save(submission);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AudioChallengeSubmissionDTO getSubmissionStatus(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .map(this::mapToSubmissionDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AudioChallengeSubmissionDTO> getUserSubmissions(Long questionId, Long userId) {
        return submissionRepository.findByQuestionIdAndUserId(questionId, userId)
                .stream()
                .map(this::mapToSubmissionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AudioChallengeSubmissionDTO getBestSubmission(Long questionId, Long userId) {
        return submissionRepository.findBestSubmissions(questionId, userId)
                .stream()
                .findFirst()
                .map(this::mapToSubmissionDTO)
                .orElse(null);
    }

    @Override
    @Transactional
    public void processPendingSubmissions() {
        List<AudioChallengeSubmission> pending = submissionRepository.findPendingSubmissions();
        log.info("📋 Processing {} pending submissions", pending.size());

        for (AudioChallengeSubmission submission : pending) {
            try {
                processSubmission(submission.getId());
            } catch (Exception e) {
                log.error("❌ Failed to process submission {}: {}",
                        submission.getId(), e.getMessage());
                markSubmissionFailed(submission.getId(), e.getMessage());
            }
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Generate a contextual default answer description for audio challenges
     * Audio challenges don't have traditional text answers - the audio IS the answer
     */
    private String generateDefaultAudioAnswer(AudioChallengeType challengeType, CreateAudioQuestionRequest request) {
        return switch (challengeType) {
            case RHYTHM_REPEAT -> buildRhythmAnswer(request);
            case KARAOKE_SING -> "[Audio vocal performance required]";
            case PITCH_MATCH -> "[Match the pitch pattern in the audio]";
            case MELODY_RECOGNITION -> "[Identify the melody]";
            case RHYTHM_RECOGNITION -> "[Identify the rhythm pattern]";
            default -> "[Audio response required]";
        };
    }

    private String buildRhythmAnswer(CreateAudioQuestionRequest request) {
        StringBuilder sb = new StringBuilder("[Rhythm pattern: ");
        if (request.getRhythmTimeSignature() != null) {
            sb.append(request.getRhythmTimeSignature());
        }
        if (request.getRhythmBpm() != null) {
            if (request.getRhythmTimeSignature() != null) sb.append(" at ");
            sb.append(request.getRhythmBpm()).append(" BPM");
        }
        if (sb.length() == "[Rhythm pattern: ".length()) {
            sb.append("repeat the pattern");
        }
        sb.append("]");
        return sb.toString();
    }

    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new IllegalArgumentException(
                    "Invalid file type. Expected audio, got: " + contentType);
        }

        // Max 100MB
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("Audio file too large. Maximum size is 100MB");
        }
    }

    private QuestionResponseDTO mapToResponseDTO(QuizQuestion question) {
        // Use existing mapping logic from QuestionService
        // This is a simplified version - adapt to your actual DTO structure
        return QuestionResponseDTO.builder()
                .id(question.getId())
                .question(question.getQuestion())
                .answer(question.getAnswer())
                .questionType(question.getQuestionType().name())
                .audioChallengeType(question.getAudioChallengeType() != null ?
                        question.getAudioChallengeType().name() : null)
                .mediaUrl(question.getQuestionMediaUrl())
                .audioSegmentStart(question.getAudioSegmentStart())
                .audioSegmentEnd(question.getAudioSegmentEnd())
                .minimumScorePercentage(question.getMinimumScorePercentage())
                .rhythmBpm(question.getRhythmBpm())
                .rhythmTimeSignature(question.getRhythmTimeSignature())
                .difficulty(question.getDifficulty() != null ? question.getDifficulty().name() : null)
                .topic(question.getTopicName())
                .creatorId(question.getCreatorId())
                .build();
    }

    private AudioChallengeSubmissionDTO mapToSubmissionDTO(AudioChallengeSubmission submission) {
        return AudioChallengeSubmissionDTO.builder()
                .id(submission.getId())
                .questionId(submission.getQuestion().getId())
                .userId(submission.getUserId())
                .processingStatus(submission.getProcessingStatus())
                .processingProgress(submission.getProcessingProgress())
                .overallScore(submission.getOverallScore())
                .pitchScore(submission.getPitchScore())
                .rhythmScore(submission.getRhythmScore())
                .voiceScore(submission.getVoiceScore())
                .passed(submission.isPassed())
                .minimumScoreRequired(submission.getQuestion().getMinimumScorePercentage())
                .detailedMetrics(submission.getDetailedMetrics())
                .createdAt(submission.getCreatedAt())
                .processedAt(submission.getProcessedAt())
                .build();
    }
}
