package com.my.challenger.web.controllers;

import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.dto.quiz.CreateQuizChallengeRequest;
import com.my.challenger.dto.quiz.QuizQuestionDTO;
import com.my.challenger.dto.quiz.SaveQuestionsRequest;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.QuizDifficulty;
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
@RequestMapping("/challenger/api/challenges")
@RequiredArgsConstructor
@Slf4j
public class QuizChallengeController {

    private final EnhancedQuizService quizService;

    /**
     * Create a quiz challenge with question saving
     */
    @PostMapping("/quiz")
    public ResponseEntity<ChallengeDTO> createQuizChallenge(
            @Valid @RequestBody CreateQuizChallengeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        ChallengeDTO challenge = quizService.createQuizChallenge(request, user.getId());
        
        return ResponseEntity.ok(challenge);
    }

    /**
     * Save additional questions for a quiz challenge
     */
    @PostMapping("/{challengeId}/questions")
    public ResponseEntity<List<QuizQuestionDTO>> saveQuestionsForChallenge(
            @PathVariable Long challengeId,
            @RequestBody SaveQuestionsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        
        // Validate challenge ownership
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));
        
        if (!challenge.getCreator().getId().equals(user.getId())) {
            throw new IllegalStateException("Only challenge creator can add questions");
        }

        List<QuizQuestionDTO> savedQuestions = quizService.saveQuizQuestions(
            request.getQuestions(), user, challenge);
        
        return ResponseEntity.ok(savedQuestions);
    }

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
        
        List<QuizQuestionDTO> questions = quizService.getQuestionsForChallenge(
            challengeId, quizDifficulty, count);
        
        return ResponseEntity.ok(questions);
    }
}
