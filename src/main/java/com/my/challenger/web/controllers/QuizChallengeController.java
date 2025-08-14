package com.my.challenger.web.controllers;

import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.dto.quiz.CreateQuizChallengeRequest;
import com.my.challenger.dto.quiz.QuizQuestionDTO;
import com.my.challenger.dto.quiz.SaveQuestionsRequest;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.repository.ChallengeRepository;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.impl.EnhancedQuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/challenger/api/challenges")
@RequiredArgsConstructor
@Slf4j
public class QuizChallengeController {

    private final EnhancedQuizService enhancedQuizService;
    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;


    /**
     * FIXED: Create quiz challenge with proper error handling
     */
    @PostMapping("/create")
    public ResponseEntity<?> createQuizChallenge(
            @Valid @RequestBody CreateQuizChallengeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("Creating quiz challenge: {} for user: {}", request.getTitle(), userDetails.getUsername());

            // Validate user
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Validate request
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Title is required"));
            }

            if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Description is required"));
            }

            // Create the challenge
            ChallengeDTO createdChallenge = enhancedQuizService.createQuizChallenge(request, user.getId());

            log.info("Successfully created quiz challenge with ID: {}", createdChallenge.getId());
            return ResponseEntity.ok(createdChallenge);

        } catch (Exception e) {
            log.error("Error creating quiz challenge: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create quiz challenge: " + e.getMessage()));
        }
    }

//    /**
//     * Get questions for a specific challenge
//     */
//    @GetMapping("/{challengeId}/questions")
//    public ResponseEntity<List<QuizQuestionDTO>> getQuestionsForChallenge(
//            @PathVariable Long challengeId,
//            @RequestParam(defaultValue = "MEDIUM") QuizDifficulty difficulty,
//            @RequestParam(defaultValue = "10") int count,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        try {
//            List<QuizQuestionDTO> questions = enhancedQuizService.getQuestionsForChallenge(
//                    challengeId, difficulty, count);
//            return ResponseEntity.ok(questions);
//        } catch (Exception e) {
//            log.error("Error getting questions for challenge {}: {}", challengeId, e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }

    /**
     * Save additional questions to an existing challenge
     */
    @PostMapping("/{challengeId}/questions")
    public ResponseEntity<?> saveQuestionsToChallenge(
            @PathVariable Long challengeId,
            @Valid @RequestBody SaveQuestionsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Verify user owns the challenge
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

            if (!challenge.getCreator().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Only challenge creator can add questions"));
            }

            enhancedQuizService.saveQuestionsToChallenge(challengeId, request.getQuestions(), user.getId());

            return ResponseEntity.ok(Map.of("message", "Questions saved successfully"));

        } catch (Exception e) {
            log.error("Error saving questions to challenge {}: {}", challengeId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to save questions: " + e.getMessage()));
        }
    }

//    /**
//     * Save additional questions for a quiz challenge
//     */
//    @PostMapping("/{challengeId}/questions")
//    public ResponseEntity<List<QuizQuestionDTO>> saveQuestionsForChallenge(
//            @PathVariable Long challengeId,
//            @RequestBody SaveQuestionsRequest request,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        User user = getUserFromUserDetails(userDetails);
//
//        // Validate challenge ownership
//        Challenge challenge = challengeRepository.findById(challengeId)
//                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));
//
//        if (!challenge.getCreator().getId().equals(user.getId())) {
//            throw new IllegalStateException("Only challenge creator can add questions");
//        }
//
//        List<QuizQuestionDTO> savedQuestions = enhancedQuizService.saveCustomQuestionsForChallenge(
//                request.getQuestions(), user, challengeId);
//
//        return ResponseEntity.ok(savedQuestions);
//    }

    /**
     * Get questions for a quiz challenge
     */
    @GetMapping("/{challengeId}/questions")
    public ResponseEntity<List<QuizQuestionDTO>> getQuestionsForChallenge(
            @PathVariable Long challengeId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "10") int count,
            @AuthenticationPrincipal UserDetails userDetails) {

        QuizDifficulty quizDifficulty = difficulty != null ?
                QuizDifficulty.valueOf(difficulty.toUpperCase()) : QuizDifficulty.MEDIUM;

        List<QuizQuestionDTO> questions = enhancedQuizService.getQuestionsForChallenge(
                challengeId, quizDifficulty, count);

        return ResponseEntity.ok(questions);
    }

    /**
     * Helper method to get User entity from UserDetails
     */
    private User getUserFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}