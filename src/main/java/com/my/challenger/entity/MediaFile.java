package com.my.challenger.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.ProcessingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a media file in the system.
 * Supports images, videos, audio, documents and other file types.
 *
 * @author Your Name
 * @version 1.0
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "media_files"
)
@EntityListeners(AuditingEntityListener.class)
@Data
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Basic file information
    @NotBlank(message = "Original filename cannot be blank")
    @Size(max = 255, message = "Original filename cannot exceed 255 characters")
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @NotBlank(message = "Filename cannot be blank")
    @Size(max = 255, message = "Filename cannot exceed 255 characters")
    @Column(name = "filename", nullable = false)
    private String filename;

    // ADD: New storage key field
    @Column(name = "storage_key", nullable = false, unique = true, updatable = false)
    private UUID storageKey;

    // ADD: Content hash for deduplication
    @Size(max = 64)
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    // ADD: Auto-generate storage key
    @PrePersist
    public void generateStorageKey() {
        if (this.storageKey == null) {
            this.storageKey = UUID.randomUUID();
        }
    }

    @NotBlank(message = "Content type cannot be blank")
    @Size(max = 100, message = "Content type cannot exceed 100 characters")
    @Column(name = "content_type", nullable = false)
    private String contentType;

    @NotNull(message = "File size cannot be null")
    @Positive(message = "File size must be positive")
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    // Media classification
    @NotNull(message = "Media type cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MediaType mediaType;

    @NotNull(message = "Media category cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "media_category", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MediaCategory mediaCategory;

    @NotNull(message = "Processing status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    // File paths and storage
    @NotBlank(message = "File path cannot be blank")
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "processed_path")
    private String processedPath;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    // Cloud storage fields
    @Column(name = "s3_key")
    private String s3Key;

    @Column(name = "s3_url")
    private String s3Url;

    // Media metadata and dimensions
    @Positive(message = "Width must be positive")
    @Column(name = "width")
    private Integer width;

    @Positive(message = "Height must be positive")
    @Column(name = "height")
    private Integer height;

    @PositiveOrZero(message = "Duration must be zero or positive")
    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @PositiveOrZero(message = "Bitrate must be zero or positive")
    @Column(name = "bitrate")
    private Long bitrate;

    @PositiveOrZero(message = "Frame rate must be zero or positive")
    @Column(name = "frame_rate")
    private Double frameRate;

    @Size(max = 50, message = "Resolution cannot exceed 50 characters")
    @Column(name = "resolution")
    private String resolution;

    // Relationships and ownership
    @Column(name = "entity_id")
    private Long entityId;

    @NotNull(message = "Uploaded by cannot be null")
    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @CreatedDate
    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @CreatedDate
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    // Utility methods
    private static String extractFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

}