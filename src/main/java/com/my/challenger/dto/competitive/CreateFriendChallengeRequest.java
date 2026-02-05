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
public class CreateFriendChallengeRequest {
    @NotNull(message = "Invitee ID is required")
    private Long inviteeId;

    @Min(value = 1, message = "At least 1 round is required")
    @Builder.Default
    private Integer totalRounds = 1;

    @NotNull(message = "Challenge type is required")
    @Builder.Default
    private AudioChallengeType audioChallengeType = AudioChallengeType.SINGING;

    private Long wagerId;
    
    private String message;
}
