package com.my.challenger.service.impl;

import com.my.challenger.entity.Photo;
import com.my.challenger.entity.enums.PhotoType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Interface for photo storage operations
 * Can be implemented for different storage backends (S3, local filesystem, etc.)
 */
public interface PhotoStorageService {

    /**
     * Save a photo to storage and database
     *
     * @param file       The uploaded file
     * @param photoType  Type of photo
     * @param entityId   ID of the entity this photo belongs to (nullable)
     * @param uploadedBy ID of the user uploading the photo
     * @return Saved photo entity
     * @throws IOException If file processing or storage fails
     */
    Photo savePhoto(MultipartFile file, PhotoType photoType, Long entityId, Long uploadedBy) throws IOException;

    /**
     * Get photo data as byte array
     *
     * @param photo The photo entity
     * @return Photo data as bytes
     * @throws IOException If retrieval fails
     */
    byte[] getPhotoData(Photo photo) throws IOException;

    /**
     * Get public URL for a photo
     *
     * @param photo The photo entity
     * @return URL string
     */
    String getPhotoUrl(Photo photo);

    /**
     * Delete a photo by ID
     *
     * @param photoId ID of the photo to delete
     * @throws IOException If deletion fails
     */
    void deletePhoto(Long photoId) throws IOException;

    /**
     * Delete a photo entity
     *
     * @param photo The photo to delete
     * @throws IOException If deletion fails
     */
    default void deletePhoto(Photo photo) throws IOException {
        deletePhoto(photo.getId());
    }

    /**
     * Get photo by ID
     *
     * @param photoId Photo ID
     * @return Optional photo
     */
    Optional<Photo> getPhoto(Long photoId);

    /**
     * Get photos by entity ID and type
     *
     * @param entityId  Entity ID
     * @param photoType Photo type
     * @return Optional photo (for single-instance types like AVATAR)
     */
    Optional<Photo> getPhotoByEntityAndType(Long entityId, PhotoType photoType);

    /**
     * Get all photos for an entity
     *
     * @param entityId Entity ID
     * @return List of photos
     */
    List<Photo> getPhotosByEntity(Long entityId);

    /**
     * Get photos uploaded by a user
     *
     * @param userId User ID
     * @return List of photos
     */
    List<Photo> getPhotosByUser(Long userId);

    /**
     * Update photo metadata
     *
     * @param photoId    Photo ID
     * @param altText    Alt text for accessibility
     * @param description Description
     * @return Updated photo
     */
    Optional<Photo> updatePhotoMetadata(Long photoId, String altText, String description);

    /**
     * Check if a photo exists
     *
     * @param photoId Photo ID
     * @return true if exists
     */
    boolean photoExists(Long photoId);

    /**
     * Get storage statistics
     *
     * @return Storage statistics (implementation specific)
     */
    default StorageStats getStorageStats() {
        return new StorageStats(0L, 0L, 0L);
    }

    /**
     * Simple storage statistics record
     */
    record StorageStats(Long totalPhotos, Long totalSizeBytes, Long totalUsers) {}
}