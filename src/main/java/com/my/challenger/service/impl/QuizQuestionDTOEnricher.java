package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.QuizQuestionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to enrich QuizQuestionDTO objects with PROXY URLs
 * This converts S3 keys stored in the database to backend proxy URLs
 * that stream media through the application instead of direct MinIO URLs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizQuestionDTOEnricher {

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Value("${app.base.url:http://localhost:8082}")
    private String baseUrl;

    /**
     * Enrich a single DTO with PROXY URLs (not direct MinIO URLs)
     */
    public QuizQuestionDTO enrichWithUrls(QuizQuestionDTO dto) {
        if (dto == null) {
            return null;
        }

        // Generate proxy URL for main media
        if (dto.getQuestionMediaUrl() != null && !dto.getQuestionMediaUrl().isEmpty()) {
            String proxyUrl = buildMediaProxyUrl(dto.getId());
            dto.setQuestionMediaUrl(proxyUrl);
            log.debug("Enriched question {} with media proxy URL: {}", dto.getId(), proxyUrl);
        }

        // Generate proxy URL for thumbnail
        if (dto.getQuestionThumbnailUrl() != null && !dto.getQuestionThumbnailUrl().isEmpty()) {
            String thumbnailProxyUrl = buildThumbnailProxyUrl(dto.getId());
            dto.setQuestionThumbnailUrl(thumbnailProxyUrl);
            log.debug("Enriched question {} with thumbnail proxy URL: {}", dto.getId(), thumbnailProxyUrl);
        }

        return dto;
    }

    /**
     * Build proxy URL for media streaming through backend
     * Format: {baseUrl}/api/media/question/{questionId}/stream
     */
    private String buildMediaProxyUrl(Long questionId) {
        return String.format("%s%s/media/question/%d/stream", baseUrl, contextPath, questionId);
    }

    /**
     * Build proxy URL for thumbnail streaming through backend
     * Format: {baseUrl}/api/media/question/{questionId}/thumbnail
     */
    private String buildThumbnailProxyUrl(Long questionId) {
        return String.format("%s%s/media/question/%d/thumbnail", baseUrl, contextPath, questionId);
    }

    /**
     * Enrich a list of DTOs with presigned URLs
     */
    public List<QuizQuestionDTO> enrichWithUrls(List<QuizQuestionDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::enrichWithUrls)
                .collect(Collectors.toList());
    }

    /**
     * Enrich a paginated result with presigned URLs
     */
    public Page<QuizQuestionDTO> enrichWithUrls(Page<QuizQuestionDTO> page) {
        if (page == null) {
            return null;
        }
        return page.map(this::enrichWithUrls);
    }
}
