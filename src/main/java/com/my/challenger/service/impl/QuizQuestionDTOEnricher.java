package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.QuizQuestionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to enrich QuizQuestionDTO objects with presigned URLs
 * This converts S3 keys stored in the database to temporary presigned URLs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizQuestionDTOEnricher {

    private final MinioMediaStorageService mediaStorageService;

    /**
     * Enrich a single DTO with presigned URLs
     */
    public QuizQuestionDTO enrichWithUrls(QuizQuestionDTO dto) {
        if (dto == null) {
            return null;
        }

        // Generate presigned URL for main media
        if (dto.getQuestionMediaUrl() != null && !dto.getQuestionMediaUrl().isEmpty()) {
            String presignedUrl = mediaStorageService.generatePresignedUrlFromKey(dto.getQuestionMediaUrl());
            dto.setQuestionMediaUrl(presignedUrl);
        }

        // Generate presigned URL for thumbnail
        if (dto.getQuestionThumbnailUrl() != null && !dto.getQuestionThumbnailUrl().isEmpty()) {
            String presignedUrl = mediaStorageService.generatePresignedUrlFromKey(dto.getQuestionThumbnailUrl());
            dto.setQuestionThumbnailUrl(presignedUrl);
        }

        return dto;
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
