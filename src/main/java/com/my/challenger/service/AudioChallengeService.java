package com.my.challenger.service;

import com.my.challenger.dto.audio.AudioChallengeConfigDTO;
import com.my.challenger.dto.audio.AudioChallengeSubmissionDTO;
import com.my.challenger.dto.audio.CreateAudioQuestionRequest;
import com.my.challenger.dto.audio.QuestionResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AudioChallengeService {

    /**
     * Create a new audio challenge question
     */
    QuestionResponseDTO createAudioQuestion(
            CreateAudioQuestionRequest request,
            MultipartFile referenceAudio,
            Long creatorId);

    /**
     * Update audio challenge configuration for a question
     */
    QuestionResponseDTO updateAudioConfig(Long questionId, AudioChallengeConfigDTO config);

    /**
     * Submit user recording for scoring
     */
    AudioChallengeSubmissionDTO submitRecording(
            Long questionId,
            Long userId,
            MultipartFile audioFile);

    /**
     * Get submission status and scores
     */
    AudioChallengeSubmissionDTO getSubmissionStatus(Long submissionId);

    /**
     * Get all submissions for a question by user
     */
    List<AudioChallengeSubmissionDTO> getUserSubmissions(Long questionId, Long userId);

    /**
     * Get best submission for a question by user
     */
    AudioChallengeSubmissionDTO getBestSubmission(Long questionId, Long userId);

    /**
     * Process pending submissions (called by scheduler)
     */
    void processPendingSubmissions();
}
