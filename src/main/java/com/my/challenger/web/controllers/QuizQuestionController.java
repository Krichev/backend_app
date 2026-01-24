// Add these endpoints to your QuizQuestionController.java
// src/main/java/com/my/challenger/web/controllers/QuizQuestionController.java

package com.my.challenger.web.controllers;

import com.my.challenger.dto.quiz.QuizQuestionDTO;
import com.my.challenger.dto.quiz.TopicResponse;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.mapper.QuizQuestionMapper;
import com.my.challenger.service.impl.QuizQuestionSearchService;
import com.my.challenger.service.impl.QuizQuestionDTOEnricher;
import com.my.challenger.service.impl.TopicService;
import com.my.challenger.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quiz-questions")
@AllArgsConstructor
@Slf4j
public class QuizQuestionController {

    private final QuizQuestionSearchService searchService;
    private final QuizQuestionDTOEnricher dtoEnricher;
    private final TopicService topicService;

    @GetMapping("/user/my-questions")
    @Operation(summary = "Get current user's questions")
    public ResponseEntity<List<QuizQuestionDTO>> getUserQuestions(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = ((UserPrincipal) userDetails).getId();
        log.info("Getting questions for user {}", userId);

        Page<QuizQuestion> entityPage = searchService.searchUserQuestions(userId, null, 0, 1000);
        List<QuizQuestionDTO> dtos = entityPage.getContent().stream()
                .map(QuizQuestionMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(dtos));
    }

    /**
     * Get all available topics
     * GET /api/quiz-questions/topics
     */
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getAvailableTopics() {
        log.info("Fetching available topics");
        List<String> topics = searchService.getAllTopics();
        return ResponseEntity.ok(topics);
    }

    /**
     * Get question by ID
     * GET /api/quiz-questions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuizQuestionDTO> getQuestionById(@PathVariable Long id) {
        log.info("Fetching question by ID: {}", id);

        QuizQuestion question = searchService.getQuestionById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));

        QuizQuestionDTO dto = QuizQuestionMapper.INSTANCE.toDTO(question);
        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(dto));
    }
}
