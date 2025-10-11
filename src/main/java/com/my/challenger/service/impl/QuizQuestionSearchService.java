package com.my.challenger.service.impl;

import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.quiz.Topic;
import com.my.challenger.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for searching and managing quiz questions
 * Demonstrates how to use the fixed QuizQuestionRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizQuestionSearchService {

    private final QuizQuestionRepository quizQuestionRepository;



    /**
     * Get all unique topics
     */
    public List<String> getAllTopics() {
        List<String> allTopics = quizQuestionRepository.findAll().stream()
                .map(QuizQuestion::getTopic)
                .filter(Objects::nonNull)
                .filter(topic -> !topic.getName().trim().isEmpty())
                .distinct()
                .map(Topic::getName)
                .sorted()
                .collect(Collectors.toList());

        log.debug("Found {} unique topics", allTopics.size());
        return allTopics;
    }

    /**
     * Get total question count
     */
    public long getTotalQuestionCount() {
        return quizQuestionRepository.count();
    }

    /**
     * Get count by difficulty
     */
    public long getCountByDifficulty(QuizDifficulty difficulty) {
        return quizQuestionRepository.countByDifficulty(difficulty);
    }

    /**
     * Get count by topic
     */
    public Map<String, Long> getCountByTopic() {
        List<QuizQuestion> allQuestions = quizQuestionRepository.findAll();

        return allQuestions.stream()
                .filter(q -> q.getTopic() != null && !q.getTopic().getName().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        quizQuestion -> quizQuestion.getTopic().getName(),
                        Collectors.counting()
                ));
    }

    /**
     * Get random questions by difficulty
     */
    public List<QuizQuestion> getRandomQuestionsByDifficulty(QuizDifficulty difficulty, int count) {
        return quizQuestionRepository.findRandomByDifficulty(difficulty.name(), count);
    }

    /**
     * Get question by ID
     */
    public Optional<QuizQuestion> getQuestionById(Long id) {
        return quizQuestionRepository.findById(id);
    }

    /**
     * Search with filters (existing method - already in your code)
     */
    public Page<QuizQuestion> searchWithFilters(
            String keyword,
            QuizDifficulty difficulty,
            String topic,
            Boolean isUserCreated,
            Pageable pageable) {

        // Clean inputs
        String cleanKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        String cleanTopic = StringUtils.hasText(topic) ? topic.trim().toLowerCase() : null;
        String difficultyStr = difficulty != null ? difficulty.name() : null;
        return quizQuestionRepository.searchWithFilters(cleanKeyword, difficulty, cleanTopic, isUserCreated, pageable);
    }

    /**
     * Search questions by keyword with pagination
     * This uses the FIXED searchByKeyword method
     */
    public List<QuizQuestion> searchQuestions(String keyword, int page, int size) {
        log.debug("Searching questions with keyword: '{}', page: {}, size: {}", keyword, page, size);
        
        if (!StringUtils.hasText(keyword)) {
            log.warn("Empty keyword provided, returning empty list");
            return List.of();
        }
        String normalizedKeyword = keyword != null && !keyword.trim().isEmpty()
                ? keyword.toLowerCase().trim()
                : null;

        Pageable pageable = PageRequest.of(page, size);
        List<QuizQuestion> results = quizQuestionRepository.searchByKeyword(keyword.trim(), pageable);
        
        log.debug("Found {} questions for keyword: '{}'", results.size(), keyword);
        return results;
    }

    /**
     * Advanced search with multiple filters
     */
    public Page<QuizQuestion> advancedSearch(String keyword,
                                           QuizDifficulty difficulty, 
                                           String topic, 
                                           Boolean isUserCreated,
                                           int page, 
                                           int size) {
        log.debug("Advanced search - keyword: '{}', difficulty: {}, topic: '{}', userCreated: {}", 
                keyword, difficulty, topic, isUserCreated);
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Clean up parameters
        String cleanKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        String cleanTopic = StringUtils.hasText(topic) ? topic.trim().toLowerCase() : null;
        String difficultyStr = difficulty != null ? difficulty.name() : null;
        return quizQuestionRepository.searchWithFilters(cleanKeyword, difficulty, cleanTopic, isUserCreated, pageable);
    }

    /**
     * Search questions for quiz generation
     */
    public List<QuizQuestion> searchForQuiz(String topic, QuizDifficulty difficulty, int count) {
        log.debug("Searching questions for quiz - topic: '{}', difficulty: {}, count: {}", topic, difficulty, count);

        String normalizedTopic = topic != null && !topic.trim().isEmpty()
                ? topic.toLowerCase().trim()
                : null;
        String difficultyStr = difficulty != null ? difficulty.name() : null;
        if (StringUtils.hasText(topic) && difficulty != null) {
            // Search by both topic and difficulty
            Page<QuizQuestion> quizQuestions = quizQuestionRepository.searchWithFilters(normalizedTopic, difficulty, topic, null, PageRequest.of(0, count));
            return quizQuestions.toList();
        } else if (difficulty != null) {
            // Search by difficulty only
            return quizQuestionRepository.findRandomByDifficulty(difficulty.name(), count);
        } else if (StringUtils.hasText(topic)) {
            // Search by topic only  
            return quizQuestionRepository.findRandomByTopic(normalizedTopic, count);
        } else {
            // Random questions
            return quizQuestionRepository.findRandomByDifficulties(
                Arrays.stream(QuizDifficulty.values()).map(Enum::name).collect(Collectors.toList()), 
                count);
        }
    }

    /**
     * Get random questions by multiple difficulties
     */
    public List<QuizQuestion> getRandomQuestions(List<QuizDifficulty> difficulties, int count) {
        log.debug("Getting {} random questions with difficulties: {}", count, difficulties);
        
        List<String> difficultyNames = difficulties.stream()
                .map(Enum::name)
                .collect(Collectors.toList());
                
        return quizQuestionRepository.findRandomByDifficulties(difficultyNames, count);
    }

    /**
     * Search questions by specific field
     */
    public List<QuizQuestion> searchByField(String searchType, String value, int page, int size) {
        log.debug("Searching by field - type: '{}', value: '{}', page: {}, size: {}", searchType, value, page, size);
        
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        
        Pageable pageable = PageRequest.of(page, size);
        String keyword = value.trim();

        String normalizedKeyword = keyword != null && !keyword.trim().isEmpty()
                ? keyword.toLowerCase().trim()
                : null;

        switch (searchType.toLowerCase()) {
            case "question":
                return quizQuestionRepository.searchByQuestionText(keyword, pageable);
            case "topic":
                return quizQuestionRepository.searchByTopicPattern(keyword, pageable);
            case "source":
                return quizQuestionRepository.findBySourceOrderByCreatedAtDesc(keyword);
            default:
                log.warn("Unknown search type: {}, falling back to general search", searchType);
                return quizQuestionRepository.searchByKeyword(keyword, pageable);
        }
    }

    /**
     * Get user's questions with search
     */
    public Page<QuizQuestion> searchUserQuestions(Long userId, String keyword, int page, int size) {
        log.debug("Searching user {} questions with keyword: '{}'", userId, keyword);
        
        Pageable pageable = PageRequest.of(page, size);
        
        if (StringUtils.hasText(keyword)) {
            // First get all user questions, then filter by keyword
            return quizQuestionRepository.searchWithFilters(keyword.trim().toLowerCase(), null, null, true, pageable);
        } else {
            // Just get user's questions
            return quizQuestionRepository.findByCreatorId(userId, pageable);
        }
    }

    /**
     * Get content suggestions for auto-complete
     */
    public ContentSuggestions getContentSuggestions(String partialInput) {
        log.debug("Getting content suggestions for: '{}'", partialInput);
        
        if (!StringUtils.hasText(partialInput) || partialInput.length() < 2) {
            return ContentSuggestions.empty();
        }
        
        // Get topic suggestions
        List<Object[]> topicCounts = quizQuestionRepository.getQuestionCountByTopic();
        List<String> topicSuggestions = topicCounts.stream()
                .map(row -> (String) row[0])
                .filter(topic -> topic != null && topic.toLowerCase().contains(partialInput.toLowerCase()))
                .limit(5)
                .collect(Collectors.toList());
        
        // Get question suggestions
        List<QuizQuestion> questionSuggestions = quizQuestionRepository.searchByQuestionText(partialInput, PageRequest.of(0, 3));
        
        return ContentSuggestions.builder()
                .topics(topicSuggestions)
                .questionPreviews(questionSuggestions.stream()
                        .map(q -> q.getQuestion().length() > 100 ? 
                            q.getQuestion().substring(0, 100) + "..." : 
                            q.getQuestion())
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Get multimedia questions
     */
    public List<QuizQuestion> getMultimediaQuestions(MediaType mediaType, QuizDifficulty difficulty, int page, int size) {
        log.debug("Getting multimedia questions - mediaType: {}, difficulty: {}", mediaType, difficulty);
        
        Pageable pageable = PageRequest.of(page, size);
        
        if (mediaType != null && difficulty != null) {
            return quizQuestionRepository.findByMediaTypeAndDifficulty(mediaType, difficulty, pageable);
        } else if (mediaType != null) {
            return quizQuestionRepository.findByQuestionMediaTypeOrderByCreatedAtDesc(mediaType, pageable);
        } else {
            return quizQuestionRepository.findQuestionsWithMedia(pageable);
        }
    }

    /**
     * Get question statistics
     */
    public QuestionStatistics getQuestionStatistics() {
        log.debug("Getting question statistics");
        
        List<Object[]> difficultyStats = quizQuestionRepository.getQuestionCountByDifficulty();
        List<Object[]> topicStats = quizQuestionRepository.getQuestionCountByTopic();
        long totalQuestions = quizQuestionRepository.count();
        
        return QuestionStatistics.builder()
                .totalQuestions(totalQuestions)
                .difficultyDistribution(difficultyStats)
                .topicDistribution(topicStats)
                .build();
    }

    /**
     * Validate and clean search input
     */
    private String cleanSearchInput(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        
        String cleaned = input.trim();
        
        // Remove special characters that might break the query
        cleaned = cleaned.replaceAll("[<>\"'%;()\\+]", "");
        
        // Limit length to prevent very long queries
        if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 100);
        }
        
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * DTO for content suggestions
     */
    @lombok.Builder
    @lombok.Data
    public static class ContentSuggestions {
        private List<String> topics;
        private List<String> questionPreviews;
        
        public static ContentSuggestions empty() {
            return ContentSuggestions.builder()
                    .topics(List.of())
                    .questionPreviews(List.of())
                    .build();
        }
    }

    /**
     * DTO for question statistics
     */
    @lombok.Builder
    @lombok.Data
    public static class QuestionStatistics {
        private long totalQuestions;
        private List<Object[]> difficultyDistribution;
        private List<Object[]> topicDistribution;
    }
}