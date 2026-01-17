// src/main/java/com/my/challenger/service/MediaService.java
package com.my.challenger.service;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface MediaService {

    /**
     * Upload quiz media for authenticated user
     */
    MediaFile uploadQuizMedia(MultipartFile file, String username);

    /**
     * Upload avatar for authenticated user
     */
    MediaFile uploadAvatar(MultipartFile file, String username);

    /**
     * Get media details by ID
     */
    MediaFile getMediaById(Long mediaId);

    /**
     * Get media URL
     */
    Map<String, String> getMediaUrls(Long mediaId);

    /**
     * Upload audio for a challenge
     */
    Map<String, Object> uploadChallengeAudio(Long challengeId, MultipartFile file, Long userId);

    /**
     * Delete media file
     */
    void deleteMedia(Long mediaId, String username);
}
