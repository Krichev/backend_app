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
public class RespondToInvitationRequest {
    @NotNull(message = "Invitation ID is required")
    private Long invitationId;

    @NotNull(message = "Response is required")
    private Boolean accepted;
}
