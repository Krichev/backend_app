package com.my.challenger.dto.competitive;

import com.my.challenger.entity.enums.AudioChallengeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinMatchmakingRequest {
    @NotNull(message = "Challenge type is required")
    private AudioChallengeType audioChallengeType;

    @Min(value = 1, message = "Preferred rounds must be at least 1")
    @Builder.Default
    private Integer preferredRounds = 1;
}
