package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.ValidationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to validate a topic")
public class ValidateTopicRequest {

    @NotNull(message = "Topic ID is required")
    @Schema(description = "Topic ID to validate", example = "1", required = true)
    private Long topicId;

    @NotNull(message = "New status is required")
    @Schema(description = "New validation status (APPROVED or REJECTED)", example = "APPROVED", required = true)
    private ValidationStatus newStatus;

    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    @Schema(description = "Rejection reason (required if REJECTED)", example = "Topic name is too generic")
    private String rejectionReason;
}
