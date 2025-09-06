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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_category", length = 20)
    private MediaCategory mediaCategory;

    // Image specific fields
    private Integer width;
    private Integer height;

    // Video/Audio specific fields
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    private Integer bitrate;

    private String resolution; // "1920x1080"

    @Column(name = "frame_rate", precision = 5, scale = 2)
    private BigDecimal frameRate;

    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 50)
    private MediaType mediaType;

    @Column(name = "entity_id")
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", length = 20)
    private ProcessingStatus processingStatus;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}