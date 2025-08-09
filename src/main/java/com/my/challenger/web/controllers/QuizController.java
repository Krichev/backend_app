package com.my.challenger.web.controllers;

import com.my.challenger.dto.MessageResponse;
import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuizSessionStatus;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.impl.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quiz Management", description = "Complete quiz and session management API")
public class QuizController {

    private final QuizService quizService;
    private final UserRepository userRepository;

    // =============================================================================
    // QUESTION MANAGEMENT ENDPOINTS
    // =============================================================================

    @PostMapping("/questions")
    @Operation(summary = "Create a new user question")
    public ResponseEntity<QuizQuestionDTO> createUserQuestion(
            @Valid @RequestBody CreateQuizQuestionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizQuestionDTO question = quizService.createUserQuestion(request, user.getId());
        log.info("User {} created question: {}", user.getId(), question.getId());
        return ResponseEntity.ok(question);
    }

    @GetMapping("/questions/me")
    @Operation(summary = "Get all questions created by current user")
    public ResponseEntity<List<QuizQuestionDTO>> getUserQuestions(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<QuizQuestionDTO> questions = quizService.getUserQuestions(user.getId());
        return ResponseEntity.ok(questions);
    }

    @DeleteMapping("/questions/{questionId}")
    @Operation(summary = "Delete a user-created question")
    public ResponseEntity<MessageResponse> deleteUserQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        quizService.deleteUserQuestion(questionId, user.getId());
        return ResponseEntity.ok(new MessageResponse("Question deleted successfully"));
    }

    @GetMapping("/questions/difficulty/{difficulty}")
    @Operation(summary = "Get questions by difficulty level")
    public ResponseEntity<List<QuizQuestionDTO>> getQuestionsByDifficulty(
            @PathVariable String difficulty,
            @RequestParam(defaultValue = "10") int count) {

        try {
            com.my.challenger.entity.enums.QuizDifficulty diff =
                    com.my.challenger.entity.enums.QuizDifficulty.valueOf(difficulty.toUpperCase());
            List<QuizQuestionDTO> questions = quizService.getQuestionsByDifficulty(diff, count);
            return ResponseEntity.ok(questions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/questions/search")
    @Operation(summary = "Search questions by keyword")
    public ResponseEntity<List<QuizQuestionDTO>> searchQuestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {

        List<QuizQuestionDTO> questions = quizService.searchQuestions(keyword, limit);
        return ResponseEntity.ok(questions);
    }

    // =============================================================================
    // QUIZ SESSION MANAGEMENT ENDPOINTS
    // =============================================================================

    @PostMapping("/sessions")
    @Operation(summary = "Start a new quiz session")
    public ResponseEntity<QuizSessionDTO> startQuizSession(
            @Valid @RequestBody StartQuizSessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.startQuizSession(request, user.getId());
        log.info("User {} started quiz session: {}", user.getId(), session.getId());
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/begin")
    @Operation(summary = "Begin an existing quiz session")
    public ResponseEntity<QuizSessionDTO> beginQuizSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.beginQuizSession(sessionId, user.getId());
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/rounds/submit")
    @Operation(summary = "Submit an answer for current round")
    public ResponseEntity<QuizRoundDTO> submitRoundAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitRoundAnswerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizRoundDTO round = quizService.submitRoundAnswer(sessionId, request, user.getId());
        return ResponseEntity.ok(round);
    }

    @PostMapping("/sessions/{sessionId}/complete")
    @Operation(summary = "Complete a quiz session")
    public ResponseEntity<QuizSessionDTO> completeQuizSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.completeQuizSession(sessionId, user.getId());
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get quiz session details")
    public ResponseEntity<QuizSessionDTO> getQuizSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.getQuizSession(sessionId, user.getId());
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/me")
    @Operation(summary = "Get current user's quiz sessions")
    public ResponseEntity<List<QuizSessionDTO>> getUserQuizSessions(
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<QuizSessionDTO> sessions = quizService.getUserQuizSessions(user.getId(), limit);
        return ResponseEntity.ok(sessions);
    }

    // =============================================================================
    // ENHANCED SESSION SEARCH & FILTERING (Fixed repository methods)
    // =============================================================================

    @GetMapping("/sessions/me/by-source")
    @Operation(summary = "Get sessions by question source")
    public ResponseEntity<List<QuizSessionDTO>> getSessionsByQuestionSource(
            @RequestParam String questionSource,
            @RequestParam(defaultValue = "false") boolean exactMatch,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<QuizSessionDTO> sessions;

        if (exactMatch) {
            sessions = quizService.getSessionsByExactQuestionSource(user.getId(), questionSource);
        } else {
            // FIXED: Now uses correct property name 'questionSource'
            sessions = quizService.getSessionsByQuestionSourceContaining(user.getId(), questionSource);
        }

        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/me/user-questions")
    @Operation(summary = "Get sessions using user-created questions")
    public ResponseEntity<List<QuizSessionDTO>> getUserQuestionSessions(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<QuizSessionDTO> sessions = quizService.getSessionsByExactQuestionSource(user.getId(), "user");
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/me/app-questions")
    @Operation(summary = "Get sessions using app-generated questions")
    public ResponseEntity<List<QuizSessionDTO>> getAppQuestionSessions(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<QuizSessionDTO> sessions = quizService.getSessionsByExactQuestionSource(user.getId(), "app");
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/me/search")
    @Operation(summary = "Search sessions with flexible criteria")
    public ResponseEntity<List<QuizSessionDTO>> searchSessions(
            @RequestParam(required = false) String questionSource,
            @RequestParam(required = false) QuizSessionStatus status,
            @RequestParam(required = false) String teamName,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);

        // Create search criteria
        QuizSessionSearchCriteria criteria = QuizSessionSearchCriteria.builder()
                .creatorId(user.getId())
                .questionSource(questionSource)
                .status(status)
                .teamNameFilter(teamName)
                .limit(limit)
                .build();

        List<QuizSessionDTO> sessions = quizService.searchSessions(criteria);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/me/recent")
    @Operation(summary = "Get recent sessions")
    public ResponseEntity<List<QuizSessionDTO>> getRecentSessions(
            @RequestParam(defaultValue = "30") int daysBack,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<QuizSessionDTO> sessions = quizService.getRecentSessions(user.getId(), daysBack);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/me/stats")
    @Operation(summary = "Get session statistics")
    public ResponseEntity<QuizSessionStatsDTO> getSessionStats(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionStatsDTO stats = quizService.getSessionStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    // =============================================================================
    // ROUND MANAGEMENT
    // =============================================================================

    @GetMapping("/sessions/{sessionId}/rounds")
    @Operation(summary = "Get all rounds for a session")
    public ResponseEntity<List<QuizRoundDTO>> getQuizRounds(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<QuizRoundDTO> rounds = quizService.getQuizRounds(sessionId, user.getId());
        return ResponseEntity.ok(rounds);
    }

    @GetMapping("/sessions/{sessionId}/current-round")
    @Operation(summary = "Get current active round")
    public ResponseEntity<QuizRoundDTO> getCurrentRound(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizRoundDTO round = quizService.getCurrentRound(sessionId, user.getId());
        return ResponseEntity.ok(round);
    }

    // =============================================================================
    // SESSION CONFIGURATION
    // =============================================================================

    @PutMapping("/sessions/{sessionId}/config")
    @Operation(summary = "Update session configuration")
    public ResponseEntity<QuizSessionDTO> updateSessionConfig(
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateQuizSessionConfigRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.updateSessionConfig(sessionId, request, user.getId());
        return ResponseEntity.ok(session);
    }

    @PutMapping("/sessions/{sessionId}/status")
    @Operation(summary = "Update session status")
    public ResponseEntity<MessageResponse> updateSessionStatus(
            @PathVariable Long sessionId,
            @RequestParam QuizSessionStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        quizService.updateSessionStatus(sessionId, status, user.getId());
        return ResponseEntity.ok(new MessageResponse("Session status updated successfully"));
    }

    // =============================================================================
    // UTILITY METHODS
    // =============================================================================

    private User getUserFromUserDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }
}