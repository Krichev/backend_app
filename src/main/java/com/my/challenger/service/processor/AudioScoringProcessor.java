package com.my.challenger.service.processor;

import com.my.challenger.entity.AudioSubmission;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.enums.AudioChallengeType;
import com.my.challenger.repository.AudioSubmissionRepository;
import com.my.challenger.service.integration.KaraokeScoringClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AudioScoringProcessor {

    private final AudioSubmissionRepository submissionRepository;
    private final KaraokeScoringClient karaokeClient;

    @Async("scoringTaskExecutor")
    public void processScoringAsync(Long submissionId) {
        log.info("⚙️ Starting async scoring for submission: {}", submissionId);
        try {
            processScoring(submissionId);
        } catch (Exception e) {
            log.error("❌ Error processing submission {}: {}", submissionId, e.getMessage(), e);
            markSubmissionFailed(submissionId, e.getMessage());
        }
    }

    @Transactional
    public void processScoring(Long submissionId) {
        log.info("⚙️ Processing submission: {}", submissionId);

        AudioSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

        submission.setProcessingStatus("PROCESSING");
        submission.setProcessingProgress(10);
        submissionRepository.save(submission);

        QuizQuestion question = submission.getQuestion();
        String challengeType = submission.getChallengeType();
        
        // Find the enum for weights
        AudioChallengeType typeEnum = AudioChallengeType.valueOf(challengeType);
        double[] weights = typeEnum.getScoringWeights();

        String refAudioKey = null;
        String refAudioBucket = null;
        
        if (question.getAudioReferenceMedia() != null) {
            refAudioKey = question.getAudioReferenceMedia().getS3Key();
            refAudioBucket = question.getAudioReferenceMedia().getBucketName();
        }

        log.info("🎤 Calling Karaoke service for submission {}", submissionId);
        submission.setProcessingProgress(30);
        submissionRepository.save(submission);

        var scoringResult = karaokeClient.scoreAudio(
                submission.getUserAudioS3Key(),
                submission.getUserAudioBucket(),
                refAudioKey,
                refAudioBucket,
                challengeType,
                question.getRhythmBpm(),
                question.getRhythmTimeSignature()
        );

        submission.setProcessingProgress(80);
        
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
        
        // Calculate pass/fail
        submission.calculatePassed();
        
        submission.setProcessingStatus("COMPLETED");
        submission.setProcessingProgress(100);
        submission.setProcessedAt(LocalDateTime.now());

        submissionRepository.save(submission);
        log.info("✅ Submission {} processed: overallScore={}, passed={}", 
                submissionId, overallScore, submission.getPassed());
    }

    @Transactional
    public void markSubmissionFailed(Long submissionId, String errorMessage) {
        submissionRepository.findById(submissionId).ifPresent(submission -> {
            submission.setProcessingStatus("FAILED");
            submission.setErrorMessage(errorMessage);
            submission.setProcessedAt(LocalDateTime.now());
            submissionRepository.save(submission);
            log.info("❌ Submission {} marked as FAILED: {}", submissionId, errorMessage);
        });
    }
}
