package com.my.challenger.web.controllers;

import com.my.challenger.dto.audio.AudioChallengeConfigDTO;
import com.my.challenger.dto.audio.AudioChallengeSubmissionDTO;
import com.my.challenger.dto.audio.CreateAudioQuestionRequest;
import com.my.challenger.dto.question.QuestionResponseDTO;
import com.my.challenger.entity.enums.AudioChallengeType;
import com.my.challenger.service.AudioChallengeService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audio-challenges")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audio Challenges", description = "Audio challenge question management and scoring")
public class AudioChallengeController {

    private final AudioChallengeService audioChallengeService;

    @GetMapping("/types")
    @Operation(summary = "Get available audio challenge types",
            description = "Returns list of supported audio challenge types with their characteristics")
    public ResponseEntity<List<Map<String, Object>>> getAudioChallengeTypes() {
        List<Map<String, Object>> types = Arrays.stream(AudioChallengeType.values())
                .map(type -> Map.<String, Object>of(
                        "type", type.name(),
                        "requiresReferenceAudio", type.requiresReferenceAudio(),
                        "usesPitchScoring", type.usesPitchScoring(),
                        "usesRhythmScoring", type.usesRhythmScoring(),
                        "usesVoiceScoring", type.usesVoiceScoring(),
                        "scoringWeights", Map.of(
                                "pitch", type.getScoringWeights()[0],
                                "rhythm", type.getScoringWeights()[1],
                                "voice", type.getScoringWeights()[2]
                        )
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(types);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create audio challenge question",
            description = "Create a new audio-based challenge question with optional reference audio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or missing required audio"),
            @ApiResponse(responseCode = "413", description = "Audio file too large")
    })
    public ResponseEntity<QuestionResponseDTO> createAudioQuestion(
            @RequestPart("request") @Valid CreateAudioQuestionRequest request,
            @RequestPart(value = "referenceAudio", required = false) MultipartFile referenceAudio,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("📝 Creating audio question: type={}, user={}",
                request.getAudioChallengeType(), userDetails.getUsername());

        Long userId = extractUserId(userDetails);
        QuestionResponseDTO response = audioChallengeService.createAudioQuestion(
                request, referenceAudio, userId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{questionId}/config")
    @Operation(summary = "Update audio challenge configuration",
            description = "Update audio configuration for an existing question")
    public ResponseEntity<QuestionResponseDTO> updateAudioConfig(
            @PathVariable Long questionId,
            @RequestBody @Valid AudioChallengeConfigDTO config) {

        log.info("🔧 Updating audio config: question={}", questionId);
        QuestionResponseDTO response = audioChallengeService.updateAudioConfig(questionId, config);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{questionId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit audio recording for scoring",
            description = "Submit a user's audio recording to be scored against the challenge")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Submission accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Invalid audio file"),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    public ResponseEntity<AudioChallengeSubmissionDTO> submitRecording(
            @Parameter(description = "Question ID") @PathVariable Long questionId,
            @Parameter(description = "User's audio recording") @RequestParam("audioFile") MultipartFile audioFile,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("🎤 Receiving submission: question={}, user={}",
                questionId, userDetails.getUsername());

        Long userId = extractUserId(userDetails);
        AudioChallengeSubmissionDTO response = audioChallengeService.submitRecording(
                questionId, userId, audioFile);

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/submissions/{submissionId}")
    @Operation(summary = "Get submission status",
            description = "Get processing status and scores for a submission")
    public ResponseEntity<AudioChallengeSubmissionDTO> getSubmissionStatus(
            @PathVariable Long submissionId) {

        AudioChallengeSubmissionDTO response = audioChallengeService.getSubmissionStatus(submissionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{questionId}/submissions")
    @Operation(summary = "Get user's submissions for a question",
            description = "Get all submissions by the current user for a specific question")
    public ResponseEntity<List<AudioChallengeSubmissionDTO>> getUserSubmissions(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        List<AudioChallengeSubmissionDTO> submissions =
                audioChallengeService.getUserSubmissions(questionId, userId);

        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/{questionId}/best")
    @Operation(summary = "Get user's best submission",
            description = "Get the highest-scoring submission by the current user")
    public ResponseEntity<AudioChallengeSubmissionDTO> getBestSubmission(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        AudioChallengeSubmissionDTO best = audioChallengeService.getBestSubmission(questionId, userId);

        if (best == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(best);
    }

    private Long extractUserId(UserDetails userDetails) {
        // Implement based on your UserDetails implementation
        // This is a placeholder - adapt to your auth setup
        return Long.parseLong(userDetails.getUsername());
    }
}
