package com.my.challenger.service;

import com.my.challenger.dto.audio.AudioChallengeConfigDTO;
import com.my.challenger.dto.audio.AudioSubmissionDTO;
import com.my.challenger.dto.audio.CreateAudioQuestionRequest;
import com.my.challenger.dto.audio.QuestionResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AudioSubmissionService {

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
    AudioSubmissionDTO submitRecording(
            Long questionId,
            Long userId,
            MultipartFile audioFile);

    /**
     * Get submission status and scores
     */
    AudioSubmissionDTO getSubmissionStatus(Long submissionId);

    /**
     * Get all submissions for a question by user
     */
    List<AudioSubmissionDTO> getUserSubmissions(Long questionId, Long userId);

    /**
     * Get best submission for a question by user
     */
    AudioSubmissionDTO getBestSubmission(Long questionId, Long userId);

    /**
     * Process pending submissions (called by scheduler)
     */
    void processPendingSubmissions();
}
