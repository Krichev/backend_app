package com.my.challenger.web.controllers;

import com.my.challenger.dto.audio.AudioChallengeConfigDTO;
import com.my.challenger.dto.audio.AudioSubmissionDTO;
import com.my.challenger.dto.audio.CreateAudioQuestionRequest;
import com.my.challenger.dto.audio.QuestionResponseDTO;
import com.my.challenger.entity.AudioSubmission;
import com.my.challenger.entity.enums.AudioChallengeType;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.AudioSubmissionRepository;
import com.my.challenger.repository.MediaFileRepository;
import com.my.challenger.service.impl.MinioMediaStorageService;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.AudioSubmissionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/questions/audio")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audio Challenges", description = "Audio challenge question management and scoring")
public class AudioSubmissionController {

    private final AudioSubmissionService audioSubmissionService;
    private final ObjectMapper objectMapper;
    private final AudioSubmissionRepository submissionRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MinioMediaStorageService mediaStorageService;

    @GetMapping("/debug/submission/{submissionId}")
    @Operation(summary = "Debug: Get submission storage details")
    public ResponseEntity<Map<String, Object>> debugSubmission(
            @PathVariable Long submissionId) {

        AudioSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("submissionId", submission.getId());
        debug.put("userAudioS3Key", submission.getUserAudioS3Key());
        debug.put("userAudioBucket", submission.getUserAudioBucket());
        debug.put("processingStatus", submission.getProcessingStatus());

        try {
            String presignedUrl = mediaStorageService.generatePresignedUrl(
                    submission.getUserAudioBucket(), submission.getUserAudioS3Key());
            debug.put("presignedUrl", presignedUrl);
        } catch (Exception e) {
            debug.put("minioError", e.getMessage());
        }

        return ResponseEntity.ok(debug);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create audio challenge question")
    public ResponseEntity<QuestionResponseDTO> createAudioQuestion(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "referenceAudio", required = false) MultipartFile referenceAudio,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        CreateAudioQuestionRequest request;
        try {
            request = objectMapper.readValue(requestJson, CreateAudioQuestionRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid request JSON: " + e.getMessage());
        }

        QuestionResponseDTO response = audioSubmissionService.createAudioQuestion(
                request, referenceAudio, userPrincipal.getId());

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{questionId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit audio recording for scoring")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Submission accepted for processing"),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    public ResponseEntity<AudioSubmissionDTO> submitRecording(
            @PathVariable Long questionId,
            @RequestParam("audioFile") MultipartFile audioFile,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("🎤 Receiving submission: question={}, user={}",
                questionId, userPrincipal.getUsername());

        AudioSubmissionDTO response = audioSubmissionService.submitRecording(
                questionId, userPrincipal.getId(), audioFile);

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/submissions/{submissionId}")
    @Operation(summary = "Get submission status")
    public ResponseEntity<AudioSubmissionDTO> getSubmissionStatus(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        AudioSubmissionDTO submission = audioSubmissionService.getSubmissionStatus(submissionId);
        
        // Security check: only own submissions or admin?
        if (!submission.getUserId().equals(userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(submission);
    }

    @GetMapping("/{questionId}/submissions")
    @Operation(summary = "Get user's submissions for a question")
    public ResponseEntity<List<AudioSubmissionDTO>> getUserSubmissions(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<AudioSubmissionDTO> submissions =
                audioSubmissionService.getUserSubmissions(questionId, userPrincipal.getId());

        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/{questionId}/best")
    @Operation(summary = "Get user's best submission")
    public ResponseEntity<AudioSubmissionDTO> getBestSubmission(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        AudioSubmissionDTO best = audioSubmissionService.getBestSubmission(questionId, userPrincipal.getId());

        if (best == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(best);
    }
}
