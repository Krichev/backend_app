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

    @GetMapping("/topics")
    @Operation(summary = "Get all topics", description = "Retrieve all active topics with pagination")
    public ResponseEntity<Page<TopicResponse>> getAllTopics(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TopicResponse> response = topicService.getTopics(pageable);
        return ResponseEntity.ok(response);
    }


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

        // Enrich with presigned URLs
        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(dtoPage));
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

        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(dtos));
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


    /**
     * Advanced search with multiple filters
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<Page<QuizQuestionDTO>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) QuizDifficulty difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) boolean isUserCreated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Advanced search - keyword: '{}', difficulty: {}, topic: '{}', userCreated: {}",
                keyword, difficulty, topic, isUserCreated);

        Page<QuizQuestion> entityPage = searchService.advancedSearch(keyword, difficulty, topic, isUserCreated, page, size);

        // Map page content to DTOs
        Page<QuizQuestionDTO> dtoPage = entityPage.map(QuizQuestionMapper.INSTANCE::toDTO);

        // Enrich with presigned URLs
        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(dtoPage));
    }

    /**
     * Search questions for quiz generation
     */
    @GetMapping("/search/for-quiz")
    public ResponseEntity<List<QuizQuestionDTO>> searchForQuiz(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) QuizDifficulty difficulty,
            @RequestParam(defaultValue = "10") int count) {

        log.info("Searching questions for quiz - topic: '{}', difficulty: {}, count: {}", topic, difficulty, count);

        List<QuizQuestion> entities = searchService.searchForQuiz(topic, difficulty, count);
        List<QuizQuestionDTO> dtos = entities.stream()
                .map(QuizQuestionMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(dtos));
    }

    /**
     * Get random questions by difficulties
     */
    @PostMapping("/search/random")
    public ResponseEntity<List<QuizQuestionDTO>> getRandomQuestions(
            @RequestBody List<QuizDifficulty> difficulties,
            @RequestParam(defaultValue = "10") int count) {

        log.info("Getting {} random questions with difficulties: {}", count, difficulties);

        List<QuizQuestion> entities = searchService.getRandomQuestions(difficulties, count);
        List<QuizQuestionDTO> dtos = entities.stream()
                .map(QuizQuestionMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(dtos));
    }

    /**
     * Search questions by specific field
     */
    @GetMapping("/search/by-field")
    public ResponseEntity<List<QuizQuestionDTO>> searchByField(
            @RequestParam String field,
            @RequestParam String value,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Searching by field - field: '{}', value: '{}', page: {}, size: {}", field, value, page, size);

        List<QuizQuestion> entities = searchService.searchByField(field, value, page, size);
        List<QuizQuestionDTO> dtos = entities.stream()
                .map(QuizQuestionMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(dtos));
    }

    /**
     * Search user's questions
     */
    @GetMapping("/search/my-questions")
    public ResponseEntity<Page<QuizQuestionDTO>> searchMyQuestions(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Searching user {} questions with keyword: '{}'", userId, keyword);

        Page<QuizQuestion> entityPage = searchService.searchUserQuestions(userId, keyword, page, size);
        Page<QuizQuestionDTO> dtoPage = entityPage.map(QuizQuestionMapper.INSTANCE::toDTO);

        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(dtoPage));
    }

    /**
     * Get content suggestions for auto-complete
     */
    @GetMapping("/search/suggestions")
    public ResponseEntity<QuizQuestionSearchService.ContentSuggestions> getContentSuggestions(
            @RequestParam String input) {

        log.debug("Getting content suggestions for: '{}'", input);

        QuizQuestionSearchService.ContentSuggestions suggestions = searchService.getContentSuggestions(input);

        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get multimedia questions
     */
    @GetMapping("/search/multimedia")
    public ResponseEntity<List<QuizQuestionDTO>> getMultimediaQuestions(
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(required = false) QuizDifficulty difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting multimedia questions - mediaType: {}, difficulty: {}", mediaType, difficulty);

        List<QuizQuestion> entities = searchService.getMultimediaQuestions(mediaType, difficulty, page, size);
        List<QuizQuestionDTO> dtos = entities.stream()
                .map(QuizQuestionMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(dtos));
    }

    /**
     * Get question statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<QuizQuestionSearchService.QuestionStatistics> getQuestionStatistics() {

        log.info("Getting question statistics");

        QuizQuestionSearchService.QuestionStatistics stats = searchService.getQuestionStatistics();

        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint to verify search functionality
     */
    @GetMapping("/search/health")
    public ResponseEntity<String> searchHealthCheck() {
        try {
            // Try a simple search to verify everything works
            searchService.searchQuestions("test", 0, 1);
            return ResponseEntity.ok("Search functionality is working correctly");
        } catch (Exception e) {
            log.error("Search health check failed", e);
            return ResponseEntity.internalServerError().body("Search functionality has issues: " + e.getMessage());
        }
    }

}


