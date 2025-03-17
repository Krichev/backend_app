package com.my.challenger.dto.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for photo verification request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoVerificationRequest {

    @NotNull(message = "Challenge ID is required")
    private Long challengeId;

    @NotBlank(message = "Base64 image data is required")
    private String base64Image;

    private String fileName;

    private String prompt;

    private String aiPrompt;
}