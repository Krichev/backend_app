package com.my.challenger.dto.vibration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    @NotNull
    private UUID sessionId;

    @NotNull
    private Long songId;

    @NotBlank
    private String answer;

    @NotNull
    private Integer responseTimeMs;

    @NotNull
    private Integer replaysUsed;
}
