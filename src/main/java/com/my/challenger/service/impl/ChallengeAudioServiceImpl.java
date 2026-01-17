package com.my.challenger.service.impl;

import com.my.challenger.dto.challenge.ChallengeAudioConfigDTO;
import com.my.challenger.dto.challenge.ChallengeAudioResponseDTO;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.exception.InvalidAudioSegmentException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.ChallengeRepository;
import com.my.challenger.repository.MediaFileRepository;
import com.my.challenger.service.ChallengeAudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeAudioServiceImpl implements ChallengeAudioService {

    private final ChallengeRepository challengeRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MinioMediaStorageService storageService;

    private void validateOwnership(Challenge challenge, Long userId) {
        if (!challenge.getCreator().getId().equals(userId)) {
            log.warn("🚫 Authorization failed: User {} attempted to modify challenge {} owned by {}",
                    userId, challenge.getId(), challenge.getCreator().getId());
            throw new AccessDeniedException("User does not have permission to modify this challenge's audio configuration");
        }
    }

    @Override
    @Transactional
    public ChallengeAudioResponseDTO updateAudioConfig(Long challengeId, ChallengeAudioConfigDTO config, Long userId) {
        log.info("🎵 Updating audio config for challenge ID: {}, requested by user: {}", challengeId, userId);

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with ID: " + challengeId));

        validateOwnership(challenge, userId);

        MediaFile audioMedia = null;
        Double totalDuration = null;

        if (config.getAudioMediaId() != null) {
            audioMedia = mediaFileRepository.findById(config.getAudioMediaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Audio media not found with ID: " + config.getAudioMediaId()));

            if (audioMedia.getMediaType() != MediaType.AUDIO) {
                throw new InvalidAudioSegmentException(
                        "Media file must be of type AUDIO, but was: " + audioMedia.getMediaType());
            }

            totalDuration = audioMedia.getDurationSeconds();
            if (totalDuration == null || totalDuration <= 0) {
                throw new InvalidAudioSegmentException("Audio duration not available");
            }

            Double startTime = config.getAudioStartTime() != null ? config.getAudioStartTime() : 0.0;
            Double endTime = config.getAudioEndTime();
            validateAudioSegment(startTime, endTime, totalDuration);

            challenge.setAudioMedia(audioMedia);
            challenge.setAudioStartTime(startTime);
            challenge.setAudioEndTime(endTime);
        } else {
            challenge.setAudioMedia(null);
            challenge.setAudioStartTime(0.0);
            challenge.setAudioEndTime(null);
        }

        if (config.getMinimumScorePercentage() != null) {
            challenge.setMinimumScorePercentage(config.getMinimumScorePercentage());
        }

        Challenge updated = challengeRepository.save(challenge);
        log.info("✅ Audio config updated for challenge ID: {}", challengeId);

        return buildAudioResponseDTO(updated, audioMedia, totalDuration);
    }

    @Override
    @Transactional(readOnly = true)
    public ChallengeAudioResponseDTO getAudioConfig(Long challengeId) {
        log.info("📖 Fetching audio config for challenge ID: {}", challengeId);

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with ID: " + challengeId));

        if (challenge.getAudioMedia() == null) {
            log.info("ℹ️ No audio configured for challenge ID: {}", challengeId);
            return null;
        }

        MediaFile audioMedia = challenge.getAudioMedia();
        Double totalDuration = audioMedia.getDurationSeconds();

        return buildAudioResponseDTO(challenge, audioMedia, totalDuration);
    }

    @Override
    @Transactional
    public void removeAudioConfig(Long challengeId, Long userId) {
        log.info("🗑️ Removing audio config for challenge ID: {}, requested by user: {}", challengeId, userId);

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with ID: " + challengeId));

        validateOwnership(challenge, userId);

        challenge.setAudioMedia(null);
        challenge.setAudioStartTime(0.0);
        challenge.setAudioEndTime(null);

        challengeRepository.save(challenge);
        log.info("✅ Audio config removed for challenge ID: {}", challengeId);
    }

    private void validateAudioSegment(Double startTime, Double endTime, Double totalDuration) {
        if (startTime == null || startTime < 0) {
            throw new InvalidAudioSegmentException("Audio start time must be >= 0");
        }
        if (startTime > totalDuration) {
            throw new InvalidAudioSegmentException(
                    String.format("Start time (%.2fs) exceeds duration (%.2fs)", startTime, totalDuration));
        }
        if (endTime != null) {
            if (endTime <= startTime) {
                throw new InvalidAudioSegmentException(
                        String.format("End time (%.2fs) must be > start time (%.2fs)", endTime, startTime));
            }
            if (endTime > totalDuration) {
                throw new InvalidAudioSegmentException(
                        String.format("End time (%.2fs) exceeds duration (%.2fs)", endTime, totalDuration));
            }
        }
        Double effectiveEnd = endTime != null ? endTime : totalDuration;
        if (effectiveEnd - startTime < 1.0) {
            throw new InvalidAudioSegmentException("Audio segment must be at least 1 second");
        }
    }

    private ChallengeAudioResponseDTO buildAudioResponseDTO(Challenge challenge, MediaFile audioMedia, Double totalDuration) {
        String audioUrl = null;
        if (audioMedia != null) {
            audioUrl = storageService.getMediaUrl(audioMedia);
        }

        return ChallengeAudioResponseDTO.builder()
                .audioMediaId(audioMedia != null ? audioMedia.getId() : null)
                .audioUrl(audioUrl)
                .audioStartTime(challenge.getAudioStartTime())
                .audioEndTime(challenge.getAudioEndTime())
                .totalDuration(totalDuration)
                .minimumScorePercentage(challenge.getMinimumScorePercentage())
                .build();
    }
}