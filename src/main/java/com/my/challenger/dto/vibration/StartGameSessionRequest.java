package com.my.challenger.dto.vibration;

import com.my.challenger.entity.enums.VibrationDifficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartGameSessionRequest {
    @NotNull
    private VibrationDifficulty difficulty;

    @NotNull
    @Min(1)
    @Max(50)
    private Integer questionCount;

    private Integer maxReplaysPerQuestion;
    private Integer guessTimeLimitSeconds;
    private List<String> categories;
    private List<Long> songIds;
}
