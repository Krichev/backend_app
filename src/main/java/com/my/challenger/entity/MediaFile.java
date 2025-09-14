package com.my.challenger.entity;

import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.ProcessingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "filename", nullable = false, unique = true)
    private String filename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "processed_path")
    private String processedPath;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_category", nullable = false)
    private MediaCategory mediaCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Media metadata fields
    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "bitrate")
    private Long bitrate;

    @Column(name = "frame_rate")
    private Double frameRate;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "s3_key")
    private String s3Key;

    @Column(name = "s3_url")
    private String s3Url;

    // **MISSING METHOD: getMimeType() - maps to contentType field**
    public String getMimeType() {
        return this.contentType;
    }

    public void setMimeType(String mimeType) {
        this.contentType = mimeType;
    }

    // Additional utility methods
    public boolean isImage() {
        return mediaCategory == MediaCategory.IMAGE;
    }

    public boolean isVideo() {
        return mediaCategory == MediaCategory.VIDEO;
    }

    public boolean isAudio() {
        return mediaCategory == MediaCategory.AUDIO;
    }

    public boolean isProcessingCompleted() {
        return processingStatus == ProcessingStatus.COMPLETED;
    }

    public boolean isProcessingFailed() {
        return processingStatus == ProcessingStatus.FAILED;
    }

    public String getFileExtension() {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }
}