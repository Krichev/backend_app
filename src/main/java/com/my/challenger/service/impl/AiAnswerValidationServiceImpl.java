package com.my.challenger.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.config.DeepSeekConfig;
import com.my.challenger.dto.quiz.AiValidationResult;
import com.my.challenger.service.AiAnswerValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class AiAnswerValidationServiceImpl implements AiAnswerValidationService {

    private final DeepSeekConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AtomicInteger requestCount = new AtomicInteger(0);

    public AiAnswerValidationServiceImpl(DeepSeekConfig config,
                                         @Qualifier("deepSeekWebClient") WebClient webClient,
                                         ObjectMapper objectMapper) {
        this.config = config;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        
        // Reset rate limit counter every minute
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> requestCount.set(0), 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public AiValidationResult validateAnswerWithAi(String userAnswer, String correctAnswer, String language) {
        if (!isAvailable()) {
            return fallbackResult("AI service unavailable or disabled");
        }

        if (requestCount.get() >= config.getRateLimitPerMinute()) {
            log.warn("DeepSeek rate limit reached ({} req/min). Falling back to local validation.", config.getRateLimitPerMinute());
            return fallbackResult("Rate limit reached");
        }

        long startTime = System.currentTimeMillis();
        requestCount.incrementAndGet();

        try {
            String prompt = buildPrompt(userAnswer, correctAnswer, language);
            
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "You are an answer validation assistant for a quiz game. Compare the user's answer with the correct answer. Determine if they are semantically equivalent (synonyms, same meaning, different wording, abbreviations, translations between languages). Respond ONLY with JSON: {\"equivalent\": true/false, \"confidence\": 0.0-1.0, \"explanation\": \"brief reason\"}"),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("response_format", Map.of("type", "json_object"));

            // Call API
            String responseBody = webClient.post()
                    .uri(config.getApiUrl())
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .block();

            return parseResponse(responseBody, startTime);

        } catch (Exception e) {
            log.error("Error calling DeepSeek API: {}", e.getMessage());
            return fallbackResult("Error: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return config.isEnabled() && StringUtils.hasText(config.getApiKey());
    }

    private String buildPrompt(String userAnswer, String correctAnswer, String language) {
        return String.format(
                "Correct answer: \"%s\"\n" +
                "User's answer: \"%s\"\n" +
                "Language context: %s\n\n" +
                "Are these answers semantically equivalent?",
                correctAnswer, userAnswer, language
        );
    }

    private AiValidationResult parseResponse(String responseBody, long startTime) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode jsonContent = objectMapper.readTree(content);

            boolean equivalent = jsonContent.path("equivalent").asBoolean(false);
            double confidence = jsonContent.path("confidence").asDouble(0.0);
            String explanation = jsonContent.path("explanation").asText("");

            return AiValidationResult.builder()
                    .equivalent(equivalent)
                    .confidence(confidence)
                    .explanation(explanation)
                    .aiUsed(true)
                    .fallbackUsed(false)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("Failed to parse DeepSeek response: {}", responseBody, e);
            return fallbackResult("Parse error");
        }
    }

    private AiValidationResult fallbackResult(String reason) {
        return AiValidationResult.builder()
                .equivalent(false)
                .confidence(0.0)
                .explanation(reason)
                .aiUsed(false)
                .fallbackUsed(true)
                .processingTimeMs(0)
                .build();
    }
}
