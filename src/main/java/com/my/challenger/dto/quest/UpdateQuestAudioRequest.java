package com.my.challenger.dto.quest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating quest audio configuration
 * Wraps the audio config with quest ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestAudioRequest {

    /**
     * ID of the quest to update
     */
    @NotNull(message = "Quest ID is required")
    private Long questId;

    /**
     * Audio configuration to apply
     */
    @Valid
    private QuestAudioConfigDTO audioConfig;
}
