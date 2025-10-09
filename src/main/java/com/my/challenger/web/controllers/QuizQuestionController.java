// Add these endpoints to your QuizQuestionController.java
// src/main/java/com/my/challenger/web/controllers/QuizQuestionController.java

package com.my.challenger.web.controllers;

import com.my.challenger.dto.quiz.QuizQuestionDTO;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.mapper.QuizQuestionMapper;
import com.my.challenger.service.impl.QuizQuestionSearchService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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

    /**
     * Search quiz questions with filters
     * GET /api/quiz-questions/search?keyword=history&difficulty=MEDIUM&topic=Science&page=0&size=50
     */
    @GetMapping("/search")
    public ResponseEntity<Page<QuizQuestionDTO>> searchQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) QuizDifficulty difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        log.info("Searching questions - keyword: '{}', difficulty: {}, topic: '{}', page: {}, size: {}", 
                keyword, difficulty, topic, page, size);

        // Create pageable with sorting
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Perform search
        Page<QuizQuestion> questionPage = searchService.searchWithFilters(
                keyword,
                difficulty,
                topic,
                null, // isUserCreated - null means all questions
                pageable
        );

        // Convert to DTOs
        Page<QuizQuestionDTO> dtoPage = questionPage.map(QuizQuestionMapper.INSTANCE::toDTO);

        return ResponseEntity.ok(dtoPage);
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
     * Get question statistics
     * GET /api/quiz-questions/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQuestionStats() {
        log.info("Fetching question statistics");

        long totalQuestions = searchService.getTotalQuestionCount();
        
        Map<QuizDifficulty, Long> byDifficulty = new HashMap<>();
        for (QuizDifficulty difficulty : QuizDifficulty.values()) {
            long count = searchService.getCountByDifficulty(difficulty);
            byDifficulty.put(difficulty, count);
        }

        Map<String, Long> byTopic = searchService.getCountByTopic();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuestions", totalQuestions);
        stats.put("byDifficulty", byDifficulty);
        stats.put("byTopic", byTopic);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get random questions (backward compatibility)
     * GET /api/quiz-questions/random?count=50&difficulty=MEDIUM
     */
    @GetMapping("/random")
    public ResponseEntity<List<QuizQuestionDTO>> getRandomQuestions(
            @RequestParam(defaultValue = "50") int count,
            @RequestParam(required = false) QuizDifficulty difficulty) {
        
        log.info("Fetching {} random questions with difficulty: {}", count, difficulty);

        List<QuizQuestion> questions;
        
        if (difficulty != null) {
            questions = searchService.getRandomQuestionsByDifficulty(difficulty, count);
        } else {
            questions = searchService.getRandomQuestions(
                    Arrays.asList(QuizDifficulty.values()), 
                    count
            );
        }

        List<QuizQuestionDTO> dtos = questions.stream()
                .map(QuizQuestionMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
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

        return ResponseEntity.ok(QuizQuestionMapper.INSTANCE.toDTO(question));
    }

}


