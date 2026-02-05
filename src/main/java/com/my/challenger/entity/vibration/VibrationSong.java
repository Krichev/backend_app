package com.my.challenger.entity.vibration;

import com.my.challenger.dto.vibration.RhythmPatternDTO;
import com.my.challenger.entity.enums.SongStatus;
import com.my.challenger.entity.enums.SongVisibility;
import com.my.challenger.entity.enums.VibrationDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vibration_songs", indexes = {
        @Index(name = "idx_vibration_songs_difficulty", columnList = "difficulty"),
        @Index(name = "idx_vibration_songs_category", columnList = "category"),
        @Index(name = "idx_vibration_songs_status", columnList = "status"),
        @Index(name = "idx_vibration_songs_creator", columnList = "creator_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VibrationSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false)
    @Builder.Default
    private UUID externalId = UUID.randomUUID();

    @Column(name = "song_title", nullable = false)
    private String songTitle;

    @Column(name = "artist", nullable = false)
    private String artist;

    @Column(name = "category")
    private String category;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private VibrationDifficulty difficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rhythm_pattern", columnDefinition = "jsonb", nullable = false)
    private RhythmPatternDTO rhythmPattern;

    @Column(name = "excerpt_duration_ms")
    private Integer excerptDurationMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "wrong_answers", columnDefinition = "jsonb", nullable = false)
    private List<String> wrongAnswers;

    @Column(name = "hint", columnDefinition = "TEXT")
    private String hint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private SongStatus status = SongStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility")
    @Builder.Default
    private SongVisibility visibility = SongVisibility.PUBLIC;

    @Column(name = "creator_id")
    private String creatorId;

    @Column(name = "play_count")
    @Builder.Default
    private Integer playCount = 0;

    @Column(name = "correct_guesses")
    @Builder.Default
    private Integer correctGuesses = 0;

    @Column(name = "total_attempts")
    @Builder.Default
    private Integer totalAttempts = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
