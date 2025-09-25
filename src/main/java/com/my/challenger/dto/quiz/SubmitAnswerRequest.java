package com.my.challenger.dto.quiz;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    
    @NotNull(message = "Round ID is required")
    private Long roundId;
    
    @NotBlank(message = "Answer is required")
    private String answer;
    
    private String playerWhoAnswered;
    private String discussionNotes;
    private Integer timeToAnswer; // in seconds
    private Boolean hintUsed;
    private Boolean voiceRecordingUsed;
}