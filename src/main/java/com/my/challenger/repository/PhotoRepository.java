package com.my.challenger.repository;

import com.my.challenger.entity.Photo;
import com.my.challenger.entity.enums.PhotoType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    /**
     * Find photos by entity ID and photo type
     */
    Optional<Photo> findByEntityIdAndPhotoType(Long entityId, PhotoType photoType);

    /**
     * Find all photos by entity ID
     */
    List<Photo> findByEntityId(Long entityId);

    /**
     * Find all photos by photo type
     */
    List<Photo> findByPhotoType(PhotoType photoType);

    /**
     * Find photos uploaded by a specific user
     */
    List<Photo> findByUploadedBy(Long uploadedBy);

    /**
     * Find photos by filename
     */
    Optional<Photo> findByFilename(String filename);

    /**
     * Find photos by S3 key
     */
    Optional<Photo> findByS3Key(String s3Key);

    /**
     * Find photos by file path
     */
    Optional<Photo> findByFilePath(String filePath);

    /**
     * Find all photos for a specific entity and uploaded by a specific user
     */
    List<Photo> findByEntityIdAndUploadedBy(Long entityId, Long uploadedBy);

    /**
     * Find photos by photo type and uploaded by a specific user
     */
    List<Photo> findByPhotoTypeAndUploadedBy(PhotoType photoType, Long uploadedBy);

    /**
     * Find photos by processing status
     */
    List<Photo> findByProcessingStatus(String processingStatus);

    /**
     * Delete photos by entity ID and photo type
     */
    void deleteByEntityIdAndPhotoType(Long entityId, PhotoType photoType);

    /**
     * Count photos by photo type
     */
    long countByPhotoType(PhotoType photoType);

    /**
     * Count photos uploaded by a user
     */
    long countByUploadedBy(Long uploadedBy);

    /**
     * Find latest photos by photo type
     */
    @Query("SELECT p FROM Photo p WHERE p.photoType = :photoType ORDER BY p.createdAt DESC")
    List<Photo> findLatestByPhotoType(@Param("photoType") PhotoType photoType);

    /**
     * Find photos with file size larger than specified
     */
    @Query("SELECT p FROM Photo p WHERE p.fileSize > :minSize")
    List<Photo> findPhotosLargerThan(@Param("minSize") Long minSize);

    /**
     * Find orphaned photos (entity_id is null or references non-existent entities)
     */
    @Query("SELECT p FROM Photo p WHERE p.entityId IS NULL")
    List<Photo> findOrphanedPhotos();
}