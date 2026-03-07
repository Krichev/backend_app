package com.my.challenger.dto.tv;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TvDisplayClaimRequest {
    @NotBlank
    private String pairingCode;
    @NotBlank
    private String roomCode;
}
