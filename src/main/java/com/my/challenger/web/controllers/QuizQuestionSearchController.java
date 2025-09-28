package com.my.challenger.web.controllers;

import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.service.impl.QuizQuestionSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * Search questions by keyword - FIXED VERSION
     * This replaces the broken searchByKeyword method
     */
    @GetMapping("/search")
    public ResponseEntity<List<QuizQuestion>> searchQuestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Searching questions with keyword: '{}', page: {}, size: {}", keyword, page, size);
        
        List<QuizQuestion> results = searchService.searchQuestions(keyword, page, size);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Advanced search with multiple filters
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<List<QuizQuestion>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) QuizDifficulty difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Boolean isUserCreated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Advanced search - keyword: '{}', difficulty: {}, topic: '{}', userCreated: {}", 
                keyword, difficulty, topic, isUserCreated);
        
        List<QuizQuestion> results = searchService.advancedSearch(keyword, difficulty, topic, isUserCreated, page, size);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Search questions for quiz generation
     */
    @GetMapping("/search/for-quiz")
    public ResponseEntity<List<QuizQuestion>> searchForQuiz(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) QuizDifficulty difficulty,
            @RequestParam(defaultValue = "10") int count) {
        
        log.info("Searching questions for quiz - topic: '{}', difficulty: {}, count: {}", topic, difficulty, count);
        
        List<QuizQuestion> results = searchService.searchForQuiz(topic, difficulty, count);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Get random questions by difficulties
     */
    @PostMapping("/search/random")
    public ResponseEntity<List<QuizQuestion>> getRandomQuestions(
            @RequestBody List<QuizDifficulty> difficulties,
            @RequestParam(defaultValue = "10") int count) {
        
        log.info("Getting {} random questions with difficulties: {}", count, difficulties);
        
        List<QuizQuestion> results = searchService.getRandomQuestions(difficulties, count);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Search questions by specific field
     */
    @GetMapping("/search/by-field")
    public ResponseEntity<List<QuizQuestion>> searchByField(
            @RequestParam String field,
            @RequestParam String value,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Searching by field - field: '{}', value: '{}', page: {}, size: {}", field, value, page, size);
        
        List<QuizQuestion> results = searchService.searchByField(field, value, page, size);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Search user's questions
     */
    @GetMapping("/search/my-questions")
    public ResponseEntity<List<QuizQuestion>> searchMyQuestions(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Searching user {} questions with keyword: '{}'", userId, keyword);
        
        List<QuizQuestion> results = searchService.searchUserQuestions(userId, keyword, page, size);
        
        return ResponseEntity.ok(results);
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
    public ResponseEntity<List<QuizQuestion>> getMultimediaQuestions(
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(required = false) QuizDifficulty difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting multimedia questions - mediaType: {}, difficulty: {}", mediaType, difficulty);
        
        List<QuizQuestion> results = searchService.getMultimediaQuestions(mediaType, difficulty, page, size);
        
        return ResponseEntity.ok(results);
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
    public ResponseEntity<List<QuizQuestion>> legacySearch(
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