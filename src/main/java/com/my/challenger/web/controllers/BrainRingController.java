package com.my.challenger.web.controllers;

import com.my.challenger.dto.quiz.BrainRingAnswerRequest;
import com.my.challenger.dto.quiz.BrainRingAnswerResponse;
import com.my.challenger.dto.quiz.BrainRingStateDTO;
import com.my.challenger.dto.quiz.BuzzRequest;
import com.my.challenger.dto.quiz.BuzzResponse;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.BrainRingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz/sessions/{sessionId}/rounds/{roundId}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Brain Ring", description = "Brain Ring game mode operations")
public class BrainRingController {

    private final BrainRingService brainRingService;

    @PostMapping("/buzz")
    @Operation(summary = "Record a buzz attempt")
    public ResponseEntity<BuzzResponse> buzz(
            @PathVariable Long sessionId,
            @PathVariable Long roundId,
            @RequestBody BuzzRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        log.info("Buzz attempt from user {} for round {}", userId, roundId);
        
        BuzzResponse response = brainRingService.processBuzz(sessionId, roundId, userId, request.getTimestamp());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/brain-ring-answer")
    @Operation(summary = "Submit an answer for Brain Ring mode")
    public ResponseEntity<BrainRingAnswerResponse> submitBrainRingAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long roundId,
            @RequestBody BrainRingAnswerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        log.info("Brain Ring answer submission from user {} for round {}", userId, roundId);
        
        BrainRingAnswerResponse response = brainRingService.submitAnswer(sessionId, roundId, userId, request.getAnswer());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/brain-ring-state")
    @Operation(summary = "Get current Brain Ring state for a round")
    public ResponseEntity<BrainRingStateDTO> getBrainRingState(
            @PathVariable Long sessionId,
            @PathVariable Long roundId) {
        
        BrainRingStateDTO state = brainRingService.getRoundState(sessionId, roundId);
        return ResponseEntity.ok(state);
    }
}
