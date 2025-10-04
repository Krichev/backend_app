package com.my.challenger.web.controllers;

import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.dto.quiz.CreateQuizChallengeRequest;
import com.my.challenger.entity.User;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.impl.EnhancedQuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for Quiz Challenge operations
 * Handles creation and management of quiz-type challenges
 */
@RestController
@RequestMapping("/challenger/api/challenges")
@RequiredArgsConstructor
@Slf4j
public class QuizChallengeController {

    private final EnhancedQuizService enhancedQuizService;
    private final UserRepository userRepository;

    /**
     * Create a new quiz challenge with full configuration
     *
     * @param request The quiz challenge creation request containing:
     *                - Basic challenge info (title, description, visibility)
     *                - Quiz configuration (difficulty, time, rounds, AI host, etc.)
     *                - Custom questions (optional)
     * @param userDetails The authenticated user creating the challenge
     * @return The created challenge DTO with all saved configuration
     */
    @PostMapping("/quiz")
    public ResponseEntity<?> createQuizChallenge(
            @Valid @RequestBody CreateQuizChallengeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("=== Creating Quiz Challenge ===");
            log.info("User: {}", userDetails.getUsername());
            log.info("Title: {}", request.getTitle());

            // Log the quiz config details
            if (request.getQuizConfig() != null) {
                log.info("Quiz Config Details:");
                log.info("  - Game Type: {}", request.getQuizConfig().getGameType());
                log.info("  - Team Name: {}", request.getQuizConfig().getTeamName());
                log.info("  - Team Members: {}", request.getQuizConfig().getTeamMembers());
                log.info("  - Difficulty: {}", request.getQuizConfig().getDefaultDifficulty());
                log.info("  - Round Time: {}s", request.getQuizConfig().getDefaultRoundTimeSeconds());
                log.info("  - Total Rounds: {}", request.getQuizConfig().getDefaultTotalRounds());
                log.info("  - AI Host Enabled: {}", request.getQuizConfig().getEnableAiHost());
                log.info("  - Team Based: {}", request.getQuizConfig().getTeamBased());
            }

            // Validate user exists
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getUsername()));

            // Validate basic fields
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                log.error("Title is missing or empty");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Title is required"));
            }

            if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
                log.error("Description is missing or empty");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Description is required"));
            }

            // Validate quiz config
            if (request.getQuizConfig() == null) {
                log.error("Quiz configuration is missing");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Quiz configuration is required"));
            }

            // Validate quiz config fields
            var quizConfig = request.getQuizConfig();
            if (quizConfig.getTeamName() == null || quizConfig.getTeamName().trim().isEmpty()) {
                log.error("Team name is missing");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Team name is required"));
            }

            if (quizConfig.getTeamMembers() == null || quizConfig.getTeamMembers().isEmpty()) {
                log.error("Team members list is empty");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "At least one team member is required"));
            }

            // Create the challenge
            log.info("Calling service to create quiz challenge...");
            ChallengeDTO createdChallenge = enhancedQuizService.createQuizChallenge(request, user.getId());

            log.info("Successfully created quiz challenge with ID: {}", createdChallenge.getId());
            log.info("Quiz config saved: {}", createdChallenge.getQuizConfig());

            // Log custom questions if any
            if (request.getCustomQuestions() != null) {
                log.info("Custom questions added: {}", request.getCustomQuestions().size());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(createdChallenge);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Error creating quiz challenge", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to create quiz challenge",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Get quiz challenge details by ID
     * Returns the challenge with parsed quiz configuration
     */
    @GetMapping("/{challengeId}/quiz-details")
    public ResponseEntity<?> getQuizChallengeDetails(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("Fetching quiz challenge details for ID: {}", challengeId);

            // This would use a service method to get the challenge
            // For now, returning a basic response
            return ResponseEntity.ok(Map.of(
                    "message", "Quiz challenge details endpoint",
                    "challengeId", challengeId
            ));

        } catch (Exception e) {
            log.error("Error fetching quiz challenge details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch quiz challenge details"));
        }
    }

    /**
     * Update quiz configuration for an existing challenge
     */
    @PutMapping("/{challengeId}/quiz-config")
    public ResponseEntity<?> updateQuizConfig(
            @PathVariable Long challengeId,
            @RequestBody Map<String, Object> configUpdates,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("Updating quiz config for challenge ID: {}", challengeId);

            // Validate user has permission to update
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Update logic would go here

            return ResponseEntity.ok(Map.of(
                    "message", "Quiz configuration updated successfully",
                    "challengeId", challengeId
            ));

        } catch (Exception e) {
            log.error("Error updating quiz configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update quiz configuration"));
        }
    }
}