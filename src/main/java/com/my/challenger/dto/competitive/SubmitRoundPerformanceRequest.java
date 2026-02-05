package com.my.challenger.dto.competitive;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitRoundPerformanceRequest {
    @NotNull(message = "Match ID is required")
    private Long matchId;

    @NotNull(message = "Round ID is required")
    private Long roundId;

    // This field might be used if the client uploads first and sends path, 
    // or if the multipart handling is done in controller and passed as argument.
    // Assuming for DTO correctness it might be passed here or handled via @RequestParam in controller.
    // Based on `AudioChallengeServiceImpl.submitRecording`, it takes `MultipartFile`. 
    // However, the doc mentions `SubmitRoundPerformanceRequest` has `audioFilePath`.
    // I will include it, but the controller might handle the file upload separately and populate this.
    private String audioFilePath; 
}
