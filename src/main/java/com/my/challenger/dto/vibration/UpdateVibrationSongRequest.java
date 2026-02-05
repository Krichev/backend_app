package com.my.challenger.dto.vibration;

import com.my.challenger.entity.enums.SongVisibility;
import com.my.challenger.entity.enums.VibrationDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVibrationSongRequest {
    private String songTitle;
    private String artist;
    private String category;
    private Integer releaseYear;
    private VibrationDifficulty difficulty;
    private RhythmPatternDTO rhythmPattern;
    private Integer excerptDurationMs;
    private List<String> wrongAnswers;
    private String hint;
    private SongVisibility visibility;
}
