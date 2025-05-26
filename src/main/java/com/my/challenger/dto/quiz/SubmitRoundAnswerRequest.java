package com.my.challenger.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitRoundAnswerRequest {
    @NotNull(message = "Round number is required")
    private Integer roundNumber;

    @NotBlank(message = "Team answer is required")
    private String teamAnswer;

    @NotBlank(message = "Player who answered is required")
    private String playerWhoAnswered;

    private String discussionNotes;
    private Boolean hintUsed = false;
    private Boolean voiceRecordingUsed = false;
}
