package com.my.challenger.web.controllers;

import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.dto.quiz.QuizQuestionDTO;
import com.my.challenger.mapper.QuizQuestionMapper;
import com.my.challenger.service.impl.QuizQuestionSearchService;
import com.my.challenger.service.impl.QuizQuestionDTOEnricher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for quiz question search operations
 * Shows how to use the fixed repository methods via service layer
 */
@RestController
@RequestMapping("/api/quiz/questions")
@RequiredArgsConstructor
@Slf4j
public class QuizQuestionSearchController {

    private final QuizQuestionSearchService searchService;
    private final QuizQuestionDTOEnricher dtoEnricher;

    /**
     * Search questions by keyword - FIXED VERSION
     * This replaces the broken searchByKeyword method
     */
    @GetMapping("/search")
    public ResponseEntity<List<QuizQuestionDTO>> searchQuestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Searching questions with keyword: '{}', page: {}, size: {}", keyword, page, size);

        List<QuizQuestion> entities = searchService.searchQuestions(keyword, page, size);

        // Map to DTOs and enrich with presigned URLs
        List<QuizQuestionDTO> dtos = entities.stream()
                .map(QuizQuestionMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(dtos));
    }

    /**
     * Advanced search with multiple filters
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<Page<QuizQuestionDTO>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) QuizDifficulty difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Boolean isUserCreated,
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
     * Get all available topics
     * GET /api/quiz/questions/topics
     */
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getAvailableTopics() {
        log.info("Fetching available topics");

        List<String> topics = searchService.getAllTopics();

        return ResponseEntity.ok(topics);
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
     * Example of how to handle the old broken endpoint
     * This shows how to migrate from the broken method to the fixed one
     */
    @GetMapping("/legacy-search")
    @Deprecated
    public ResponseEntity<List<QuizQuestionDTO>> legacySearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.warn("Legacy search endpoint called, redirecting to new search method");

        // Redirect to the new working method
        return searchQuestions(keyword, page, size);
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