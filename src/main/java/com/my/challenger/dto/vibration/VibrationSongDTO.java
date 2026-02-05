package com.my.challenger.dto.vibration;

import com.my.challenger.entity.enums.SongStatus;
import com.my.challenger.entity.enums.SongVisibility;
import com.my.challenger.entity.enums.VibrationDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VibrationSongDTO {
    private Long id;
    private UUID externalId;
    private String songTitle;
    private String artist;
    private String category;
    private Integer releaseYear;
    private VibrationDifficulty difficulty;
    private RhythmPatternDTO rhythmPattern;
    private Integer excerptDurationMs;
    private List<String> wrongAnswers;
    private String hint;
    private SongStatus status;
    private SongVisibility visibility;
    private String creatorId;
    private String creatorUsername;
    private Integer playCount;
    private Double averageScore;
    private Integer correctGuesses;
    private Integer totalAttempts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
