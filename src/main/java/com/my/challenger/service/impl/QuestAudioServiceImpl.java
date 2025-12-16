package com.my.challenger.service.impl;

import com.my.challenger.dto.quest.QuestAudioConfigDTO;
import com.my.challenger.dto.quest.QuestAudioResponseDTO;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.Quest;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.exception.InvalidAudioSegmentException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.MediaFileRepository;
import com.my.challenger.repository.QuestRepository;
import com.my.challenger.service.QuestAudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing quest audio configuration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestAudioServiceImpl implements QuestAudioService {

    private final QuestRepository questRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MediaProcessingService mediaProcessingService;

    @Override
    @Transactional
    public QuestAudioResponseDTO updateAudioConfig(Long questId, QuestAudioConfigDTO config) {
        log.info("🎵 Updating audio config for quest ID: {}", questId);

        // 1. Load quest
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found with ID: " + questId));

        // 2. If audio media ID is provided, validate and load media file
        MediaFile audioMedia = null;
        Double totalDuration = null;

        if (config.getAudioMediaId() != null) {
            audioMedia = mediaFileRepository.findById(config.getAudioMediaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Audio media not found with ID: " + config.getAudioMediaId()));

            // 3. Validate media is AUDIO type
            if (audioMedia.getMediaType() != MediaType.AUDIO) {
                throw new InvalidAudioSegmentException(
                        "Media file must be of type AUDIO, but was: " + audioMedia.getMediaType());
            }

            // 4. Get audio duration
            totalDuration = audioMedia.getDurationSeconds();
            if (totalDuration == null || totalDuration <= 0) {
                log.warn("⚠️ Audio duration not available in MediaFile, attempting extraction");
                // Fallback: try to extract duration if not stored
                try {
                    if (audioMedia.getFilePath() != null) {
                        var metadata = mediaProcessingService.extractAudioMetadata(audioMedia.getFilePath());
                        totalDuration = metadata.getDurationSeconds();
                        // Update MediaFile with extracted duration
                        audioMedia.setDurationSeconds(totalDuration);
                        mediaFileRepository.save(audioMedia);
                    } else {
                        throw new InvalidAudioSegmentException("Audio duration not available");
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to extract audio duration", e);
                    throw new InvalidAudioSegmentException("Failed to determine audio duration: " + e.getMessage());
                }
            }

            // 5. Validate segment times
            Double startTime = config.getAudioStartTime() != null ? config.getAudioStartTime() : 0.0;
            Double endTime = config.getAudioEndTime();

            validateAudioSegment(startTime, endTime, totalDuration);

            // 6. Update quest entity
            quest.setAudioMedia(audioMedia);
            quest.setAudioStartTime(startTime);
            quest.setAudioEndTime(endTime);
        } else {
            // Remove audio configuration
            quest.setAudioMedia(null);
            quest.setAudioStartTime(0.0);
            quest.setAudioEndTime(null);
        }

        // 7. Update minimum score percentage
        if (config.getMinimumScorePercentage() != null) {
            quest.setMinimumScorePercentage(config.getMinimumScorePercentage());
        }

        // 8. Save quest
        Quest updatedQuest = questRepository.save(quest);

        log.info("✅ Audio config updated for quest ID: {}", questId);

        // 9. Build and return response DTO
        return buildAudioResponseDTO(updatedQuest, audioMedia, totalDuration);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestAudioResponseDTO getAudioConfig(Long questId) {
        log.info("📖 Fetching audio config for quest ID: {}", questId);

        // Load quest with audio media eagerly loaded
        Quest quest = questRepository.findByIdWithAudioMedia(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found with ID: " + questId));

        if (quest.getAudioMedia() == null) {
            log.info("ℹ️ No audio configured for quest ID: {}", questId);
            return null;
        }

        MediaFile audioMedia = quest.getAudioMedia();
        Double totalDuration = audioMedia.getDurationSeconds();

        return buildAudioResponseDTO(quest, audioMedia, totalDuration);
    }

    @Override
    @Transactional
    public void removeAudioConfig(Long questId) {
        log.info("🗑️ Removing audio config for quest ID: {}", questId);

        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found with ID: " + questId));

        quest.setAudioMedia(null);
        quest.setAudioStartTime(0.0);
        quest.setAudioEndTime(null);

        questRepository.save(quest);

        log.info("✅ Audio config removed for quest ID: {}", questId);
    }

    /**
     * Validate audio segment configuration
     */
    private void validateAudioSegment(Double startTime, Double endTime, Double totalDuration) {
        // Validate start time
        if (startTime == null || startTime < 0) {
            throw new InvalidAudioSegmentException("Audio start time must be >= 0");
        }

        // Validate start time doesn't exceed duration
        if (startTime > totalDuration) {
            throw new InvalidAudioSegmentException(
                    String.format("Audio start time (%.2fs) exceeds total duration (%.2fs)",
                            startTime, totalDuration));
        }

        // Validate end time if provided
        if (endTime != null) {
            if (endTime <= startTime) {
                throw new InvalidAudioSegmentException(
                        String.format("Audio end time (%.2fs) must be greater than start time (%.2fs)",
                                endTime, startTime));
            }

            if (endTime > totalDuration) {
                throw new InvalidAudioSegmentException(
                        String.format("Audio end time (%.2fs) exceeds total duration (%.2fs)",
                                endTime, totalDuration));
            }
        }

        // Validate minimum segment length (at least 1 second)
        Double effectiveEndTime = endTime != null ? endTime : totalDuration;
        Double segmentDuration = effectiveEndTime - startTime;

        if (segmentDuration < 1.0) {
            throw new InvalidAudioSegmentException(
                    String.format("Audio segment must be at least 1 second long, but was %.2fs",
                            segmentDuration));
        }

        log.info("✅ Audio segment validation passed: start=%.2fs, end=%.2fs, duration=%.2fs",
                startTime, effectiveEndTime, segmentDuration);
    }

    /**
     * Build audio response DTO from quest and media entities
     */
    private QuestAudioResponseDTO buildAudioResponseDTO(Quest quest, MediaFile audioMedia, Double totalDuration) {
        if (audioMedia == null) {
            return null;
        }

        // Build audio URL for streaming
        String audioUrl = String.format("/api/media/stream/%d", audioMedia.getId());

        return QuestAudioResponseDTO.builder()
                .audioMediaId(audioMedia.getId())
                .audioUrl(audioUrl)
                .audioStartTime(quest.getAudioStartTime() != null ? quest.getAudioStartTime() : 0.0)
                .audioEndTime(quest.getAudioEndTime())
                .totalDuration(totalDuration)
                .minimumScorePercentage(quest.getMinimumScorePercentage() != null ?
                        quest.getMinimumScorePercentage() : 0)
                .build();
    }
}
