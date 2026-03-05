package com.my.challenger.service.impl;

import com.my.challenger.dto.audio.AudioChallengeConfigDTO;
import com.my.challenger.dto.audio.AudioSubmissionDTO;
import com.my.challenger.dto.audio.CreateAudioQuestionRequest;
import com.my.challenger.dto.audio.QuestionResponseDTO;
import com.my.challenger.entity.AudioSubmission;
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
import com.my.challenger.repository.AudioSubmissionRepository;
import com.my.challenger.repository.MediaFileRepository;
import com.my.challenger.repository.QuizQuestionRepository;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.AudioSubmissionService;
import com.my.challenger.service.TopicService;
import com.my.challenger.service.processor.AudioScoringProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioSubmissionServiceImpl implements AudioSubmissionService {

    private final QuizQuestionRepository questionRepository;
    private final AudioSubmissionRepository submissionRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final TopicService topicService;
    private final MinioMediaStorageService mediaStorageService;
    private final AudioScoringProcessor scoringProcessor;

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
        }

        // 4. Get or create topic
        Topic topic = null;
        if (request.getTopic() != null && !request.getTopic().isBlank()) {
            topic = topicService.getOrCreateTopic(request.getTopic());
        }

        // 5. Build question entity
        QuizQuestion question = QuizQuestion.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer() != null ? request.getAnswer() : "[Audio response required]")
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
                .timeLimitSeconds(request.getTimeLimitSeconds())
                .creator(creator)
                .isUserCreated(true)
                .build();

        QuizQuestion savedQuestion = questionRepository.save(question);
        return mapToResponseDTO(savedQuestion);
    }

    @Override
    @Transactional
    public QuestionResponseDTO updateAudioConfig(Long questionId, AudioChallengeConfigDTO config) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        if (config.getAudioChallengeType() != null) {
            question.setAudioChallengeType(config.getAudioChallengeType());
        }

        if (config.getAudioReferenceMediaId() != null) {
            MediaFile audioMedia = mediaFileRepository.findById(config.getAudioReferenceMediaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Audio media not found: " + config.getAudioReferenceMediaId()));
            question.setAudioReferenceMedia(audioMedia);
            question.setQuestionMediaUrl(audioMedia.getS3Key());
            question.setQuestionMediaId(audioMedia.getId());
        }

        if (config.getAudioSegmentStart() != null) {
            question.setAudioSegmentStart(config.getAudioSegmentStart());
        }
        question.setAudioSegmentEnd(config.getAudioSegmentEnd());

        if (config.getMinimumScorePercentage() != null) {
            question.setMinimumScorePercentage(config.getMinimumScorePercentage());
        }

        if (config.getRhythmBpm() != null) {
            question.setRhythmBpm(config.getRhythmBpm());
        }
        if (config.getRhythmTimeSignature() != null) {
            question.setRhythmTimeSignature(config.getRhythmTimeSignature());
        }

        QuizQuestion saved = questionRepository.save(question);
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public AudioSubmissionDTO submitRecording(
            Long questionId,
            Long userId,
            MultipartFile audioFile) {

        log.info("🎤 Receiving submission: question={}, user={}", questionId, userId);

        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        if (!question.isAudioChallenge()) {
            throw new IllegalArgumentException("Question is not an audio challenge");
        }

        validateAudioFile(audioFile);

        // Store submission audio
        MediaFile submissionMedia = mediaStorageService.storeMedia(
                audioFile, null, MediaCategory.CHALLENGE_PROOF, userId);

        // Create submission record according to doc.md requirements
        AudioSubmission submission = AudioSubmission.builder()
                .question(question)
                .userId(userId)
                .userAudioS3Key(submissionMedia.getS3Key())
                .userAudioBucket(submissionMedia.getBucketName())
                .submissionMediaId(submissionMedia.getId())
                .challengeType(question.getAudioChallengeType().name())
                .minimumScoreRequired(question.getMinimumScorePercentage())
                .processingStatus("PENDING")
                .processingProgress(0)
                .build();

        submission = submissionRepository.save(submission);
        log.info("🎤 Submission {} saved as PENDING", submission.getId());

        // Trigger async scoring using the processor to ensure @Async works correctly
        scoringProcessor.processScoringAsync(submission.getId());

        return mapToSubmissionDTO(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public AudioSubmissionDTO getSubmissionStatus(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .map(this::mapToSubmissionDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AudioSubmissionDTO> getUserSubmissions(Long questionId, Long userId) {
        return submissionRepository.findByQuestionIdAndUserIdOrderByCreatedAtDesc(questionId, userId)
                .stream()
                .map(this::mapToSubmissionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AudioSubmissionDTO getBestSubmission(Long questionId, Long userId) {
        return submissionRepository.findBestSubmission(questionId, userId)
                .map(this::mapToSubmissionDTO)
                .orElse(null);
    }

    @Override
    @Transactional
    public void processPendingSubmissions() {
        List<AudioSubmission> pending = submissionRepository.findPendingSubmissions();
        log.info("📋 Processing {} pending submissions", pending.size());

        for (AudioSubmission submission : pending) {
            scoringProcessor.processScoringAsync(submission.getId());
        }
    }

    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("audio/") && !contentType.equals("application/octet-stream"))) {
            // Some clients send application/octet-stream for audio
            log.warn("Accepting octet-stream as audio for file: {}", file.getOriginalFilename());
        }
        // Max 100MB
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("Audio file too large. Maximum size is 100MB");
        }
    }

    private QuestionResponseDTO mapToResponseDTO(QuizQuestion question) {
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
                .timeLimitSeconds(question.getTimeLimitSeconds())
                .creatorId(question.getCreatorId())
                .build();
    }

    private AudioSubmissionDTO mapToSubmissionDTO(AudioSubmission submission) {
        return AudioSubmissionDTO.builder()
                .id(submission.getId())
                .questionId(submission.getQuestion().getId())
                .userId(submission.getUserId())
                .processingStatus(submission.getProcessingStatus())
                .processingProgress(submission.getProcessingProgress())
                .overallScore(submission.getOverallScore())
                .pitchScore(submission.getPitchScore())
                .rhythmScore(submission.getRhythmScore())
                .voiceScore(submission.getVoiceScore())
                .passed(submission.getPassed())
                .minimumScoreRequired(submission.getMinimumScoreRequired())
                .detailedMetrics(submission.getDetailedMetrics())
                .createdAt(submission.getCreatedAt())
                .processedAt(submission.getProcessedAt())
                .build();
    }
}
