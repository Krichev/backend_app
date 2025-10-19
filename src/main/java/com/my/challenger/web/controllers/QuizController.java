package com.my.challenger.web.controllers;

import com.my.challenger.dto.MessageResponse;
import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuestionVisibility;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuizSessionStatus;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.impl.QuestionService;
import com.my.challenger.service.impl.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
    private final QuestionService questionService;
    private final UserRepository userRepository;


    @PostMapping("/questions")
    @Operation(summary = "Create a user question with visibility policy")
    public ResponseEntity<QuizQuestionDTO> createUserQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateQuizQuestionRequest request) {

        Long userId = ((UserPrincipal) userDetails).getId();
        QuizQuestionDTO question = questionService.createUserQuestion(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(question);
    }

    @GetMapping("/questions/me")
    @Operation(summary = "Get my questions with pagination")
    public ResponseEntity<Page<QuizQuestionDTO>> getMyQuestions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

         Long userId = ((UserPrincipal) userDetails).getId();
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<QuizQuestionDTO> questions = questionService.getUserQuestions(userId, pageable);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/questions/accessible")
    @Operation(summary = "Search accessible questions (includes public, friends, and quiz-specific)")
    public ResponseEntity<Page<QuizQuestionDTO>> searchAccessibleQuestions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Long quizId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

         Long userId = ((UserPrincipal) userDetails).getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        QuestionSearchRequest searchRequest = QuestionSearchRequest.builder()
                .keyword(keyword)
                .difficulty(difficulty != null ? QuizDifficulty.valueOf(difficulty) : null)
                .topic(topic)
                .quizId(quizId)
                .pageable(pageable)
                .build();

        Page<QuizQuestionDTO> questions = questionService.searchAccessibleQuestions(userId, searchRequest);
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


    @PutMapping("/questions/{questionId}/visibility")
    @Operation(summary = "Update question visibility policy")
    public ResponseEntity<QuizQuestionDTO> updateQuestionVisibility(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long questionId,
            @RequestParam QuestionVisibility visibility,
            @RequestParam(required = false) Long originalQuizId) {

         Long userId = ((UserPrincipal) userDetails).getId();
        QuizQuestionDTO question = questionService.updateQuestionVisibility(
                questionId, userId, visibility, originalQuizId);
        return ResponseEntity.ok(question);
    }

    @DeleteMapping("/questions/{questionId}")
    @Operation(summary = "Delete a user question")
    public ResponseEntity<Void> deleteUserQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long questionId) {

         Long userId = ((UserPrincipal) userDetails).getId();
        questionService.deleteUserQuestion(questionId, userId);
        return ResponseEntity.noContent().build();
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