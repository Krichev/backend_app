package com.my.challenger.dto.vibration;

import com.my.challenger.entity.enums.VibrationDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VibrationSongSummaryDTO {
    private Long id;
    private UUID externalId;
    private String songTitle;
    private String artist;
    private String category;
    private VibrationDifficulty difficulty;
    private Integer playCount;
    private Integer correctGuesses;
    private Integer totalAttempts;
}
