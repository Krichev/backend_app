package com.my.challenger.repository;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    /**
     * Existing methods from your current repository
     */
    List<MediaFile> findByEntityIdAndMediaType(Long entityId, MediaType mediaType);

    List<MediaFile> findByUploadedBy(Long uploadedBy);

    List<MediaFile> findByProcessingStatus(ProcessingStatus status);

    Optional<MediaFile> findByFilename(String filename);

    Optional<MediaFile> findByS3Key(String s3Key);

    void deleteByEntityIdAndMediaType(Long entityId, MediaType mediaType);

    /**
     * **MISSING METHOD: Find media files by media type and uploader**
     * This method was being called in MediaStorageService.getMediaByTypeAndUser()
     */
    List<MediaFile> findByMediaTypeAndUploadedBy(MediaType mediaType, Long uploadedBy);

    /**
     * Additional useful methods for media management
     */
    List<MediaFile> findByEntityId(Long entityId);

    List<MediaFile> findByMediaCategory(MediaCategory mediaCategory);

    List<MediaFile> findByUploadedAtAfter(LocalDateTime uploadedAt);

    List<MediaFile> findByEntityIdAndMediaCategory(Long entityId, MediaCategory mediaCategory);

    List<MediaFile> findByUploadedByAndMediaCategory(Long uploadedBy, MediaCategory mediaCategory);

    /**
     * Calculate total file size by uploader
     */
    @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM MediaFile m WHERE m.uploadedBy = :uploadedBy")
    Long getTotalFileSizeByUploader(@Param("uploadedBy") Long uploadedBy);

    /**
     * Calculate total file size by uploader and media type
     */
    @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM MediaFile m WHERE m.uploadedBy = :uploadedBy AND m.mediaType = :mediaType")
    Long getTotalFileSizeByUploaderAndType(@Param("uploadedBy") Long uploadedBy, @Param("mediaType") MediaType mediaType);

    /**
     * Count media files by uploader
     */
    Long countByUploadedBy(Long uploadedBy);

    /**
     * Count media files by uploader and media type
     */
    Long countByUploadedByAndMediaType(Long uploadedBy, MediaType mediaType);

    /**
     * Find media files requiring processing
     */
    List<MediaFile> findByProcessingStatusIn(List<ProcessingStatus> statuses);

    /**
     * Delete media files by entity ID
     */
    void deleteByEntityId(Long entityId);

    /**
     * Find media files older than specified date
     */
    @Query("SELECT m FROM MediaFile m WHERE m.uploadedAt < :cutoffDate")
    List<MediaFile> findOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}