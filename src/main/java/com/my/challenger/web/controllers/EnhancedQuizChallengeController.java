package com.my.challenger.web.controllers;

import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.dto.MessageResponse;
import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuizDifficulty;
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

@RestController
@RequestMapping("/api/quiz/challenges")
@RequiredArgsConstructor
@Slf4j
public class EnhancedQuizChallengeController {

    private final EnhancedQuizService enhancedQuizService;
    private final UserRepository userRepository;

    /**
     * Create a new quiz challenge with custom questions
     */
    @PostMapping
    public ResponseEntity<ChallengeDTO> createQuizChallenge(
            @Valid @RequestBody CreateQuizChallengeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        ChallengeDTO challenge = enhancedQuizService.createQuizChallenge(request, user.getId());
        return ResponseEntity.ok(challenge);
    }

    /**
     * Get questions for a specific challenge
     */
    @GetMapping("/{challengeId}/questions")
    public ResponseEntity<List<QuizQuestionDTO>> getQuestionsForChallenge(
            @PathVariable Long challengeId,
            @RequestParam(defaultValue = "EASY") String difficulty,
            @RequestParam(defaultValue = "10") int count) {
        
        try {
            QuizDifficulty diff = QuizDifficulty.valueOf(difficulty.toUpperCase());
            List<QuizQuestionDTO> questions = enhancedQuizService.getQuestionsForChallenge(
                    challengeId, diff, count);
            return ResponseEntity.ok(questions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create a quiz session for a specific challenge
     */
    @PostMapping("/{challengeId}/sessions")
    public ResponseEntity<QuizSessionDTO> createQuizSessionForChallenge(
            @PathVariable Long challengeId,
            @Valid @RequestBody QuizSessionConfig sessionConfig,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = enhancedQuizService.createQuizSessionForChallenge(
                challengeId, user.getId(), sessionConfig);
        return ResponseEntity.ok(session);
    }

    /**
     * Get all quiz sessions for a challenge
     */
    @GetMapping("/{challengeId}/sessions")
    public ResponseEntity<List<QuizSessionDTO>> getQuizSessionsForChallenge(
            @PathVariable Long challengeId) {
        
        List<QuizSessionDTO> sessions = enhancedQuizService.getQuizSessionsForChallenge(challengeId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get detailed quiz session results
     */
    @GetMapping("/{challengeId}/sessions/{sessionId}/details")
    public ResponseEntity<QuizSessionDetailDTO> getQuizSessionDetail(
            @PathVariable Long challengeId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        QuizSessionDetailDTO details = enhancedQuizService.getQuizSessionDetail(sessionId, user.getId());
        return ResponseEntity.ok(details);
    }

    /**
     * Update quiz session configuration (only for sessions that haven't started)
     */
    @PutMapping("/{challengeId}/sessions/{sessionId}/config")
    public ResponseEntity<QuizSessionDTO> updateQuizSessionConfig(
            @PathVariable Long challengeId,
            @PathVariable Long sessionId,
            @Valid @RequestBody QuizSessionConfig newConfig,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = enhancedQuizService.updateQuizSessionConfig(
                sessionId, user.getId(), newConfig);
        return ResponseEntity.ok(session);
    }

    /**
     * Archive a completed quiz session
     */
    @PostMapping("/{challengeId}/sessions/{sessionId}/archive")
    public ResponseEntity<MessageResponse> archiveQuizSession(
            @PathVariable Long challengeId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        enhancedQuizService.archiveQuizSession(sessionId, user.getId());
        return ResponseEntity.ok(new MessageResponse("Quiz session archived successfully"));
    }

    /**
     * Get quiz challenge statistics
     */
    @GetMapping("/{challengeId}/stats")
    public ResponseEntity<QuizChallengeStatsDTO> getQuizChallengeStats(
            @PathVariable Long challengeId) {
        
        QuizChallengeStatsDTO stats = enhancedQuizService.getQuizChallengeStats(challengeId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Save custom questions for a challenge
     */
    @PostMapping("/{challengeId}/questions")
    public ResponseEntity<List<QuizQuestionDTO>> saveCustomQuestionsForChallenge(
            @PathVariable Long challengeId,
            @Valid @RequestBody SaveQuestionsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        List<QuizQuestionDTO> questions = enhancedQuizService.saveCustomQuestionsForChallenge(
                request.getQuestions(), user, challengeId);
        return ResponseEntity.ok(questions);
    }

    /**
     * Helper method to get User entity from UserDetails
     */
    private User getUserFromUserDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}

