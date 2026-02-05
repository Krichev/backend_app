package com.my.challenger.dto.vibration;

import com.my.challenger.entity.enums.SongVisibility;
import com.my.challenger.entity.enums.VibrationDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVibrationSongRequest {
    @NotBlank
    private String songTitle;

    @NotBlank
    private String artist;

    private String category;
    private Integer releaseYear;

    @NotNull
    private VibrationDifficulty difficulty;

    @NotNull
    private RhythmPatternDTO rhythmPattern;

    private Integer excerptDurationMs;

    @NotNull
    @Size(min = 3, max = 3)
    private List<String> wrongAnswers;

    private String hint;
    private SongVisibility visibility;
}
