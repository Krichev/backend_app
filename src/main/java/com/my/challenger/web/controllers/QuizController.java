// src/main/java/com/my/challenger/web/controllers/QuizController.java
package com.my.challenger.web.controllers;

import com.my.challenger.dto.MessageResponse;
import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.User;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.impl.QuizService;
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
public class QuizController {

    private final QuizService quizService;
    private final UserRepository userRepository;

    // Question Management Endpoints
    @PostMapping("/questions")
    public ResponseEntity<QuizQuestionDTO> createUserQuestion(
            @Valid @RequestBody CreateQuizQuestionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        QuizQuestionDTO question = quizService.createUserQuestion(request, user.getId());
        return ResponseEntity.ok(question);
    }

    @GetMapping("/questions/me")
    public ResponseEntity<List<QuizQuestionDTO>> getUserQuestions(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        List<QuizQuestionDTO> questions = quizService.getUserQuestions(user.getId());
        return ResponseEntity.ok(questions);
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<MessageResponse> deleteUserQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        quizService.deleteUserQuestion(questionId, user.getId());
        return ResponseEntity.ok(new MessageResponse("Question deleted successfully"));
    }

    @GetMapping("/questions/difficulty/{difficulty}")
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
    public ResponseEntity<List<QuizQuestionDTO>> searchQuestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        
        List<QuizQuestionDTO> questions = quizService.searchQuestions(keyword, limit);
        return ResponseEntity.ok(questions);
    }

    // Quiz Session Management Endpoints
    @PostMapping("/sessions")
    public ResponseEntity<QuizSessionDTO> startQuizSession(
            @Valid @RequestBody StartQuizSessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.startQuizSession(request, user.getId());
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/begin")
    public ResponseEntity<QuizSessionDTO> beginQuizSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.beginQuizSession(sessionId, user.getId());
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/rounds/submit")
    public ResponseEntity<QuizRoundDTO> submitRoundAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitRoundAnswerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        QuizRoundDTO round = quizService.submitRoundAnswer(sessionId, request, user.getId());
        return ResponseEntity.ok(round);
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public ResponseEntity<QuizSessionDTO> completeQuizSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.completeQuizSession(sessionId, user.getId());
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<QuizSessionDTO> getQuizSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.getQuizSession(sessionId, user.getId());
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/me")
    public ResponseEntity<List<QuizSessionDTO>> getUserQuizSessions(
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        List<QuizSessionDTO> sessions = quizService.getUserQuizSessions(user.getId(), limit);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/{sessionId}/rounds")
    public ResponseEntity<List<QuizRoundDTO>> getQuizRounds(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        List<QuizRoundDTO> rounds = quizService.getQuizRounds(sessionId, user.getId());
        return ResponseEntity.ok(rounds);
    }

    private User getUserFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}