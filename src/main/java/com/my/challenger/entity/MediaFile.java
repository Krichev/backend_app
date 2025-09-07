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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private ProcessingStatus processingStatus;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    // Media metadata
    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "bitrate")
    private Long bitrate;

    @Column(name = "frame_rate", precision = 10, scale = 2)
    private BigDecimal frameRate;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "s3_key")
    private String s3Key;

    @Column(name = "s3_url")
    private String s3Url;
}