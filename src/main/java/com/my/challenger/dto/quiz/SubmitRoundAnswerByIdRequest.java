package com.my.challenger.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitRoundAnswerByIdRequest {
    @NotBlank(message = "Team answer is required")
    private String teamAnswer;
    
    private String playerWhoAnswered;
    private String discussionNotes;
    private Boolean hintUsed;
    private Boolean voiceRecordingUsed;
}
