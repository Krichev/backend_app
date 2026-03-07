package com.my.challenger.dto.tv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TvDisplayRegistrationDTO {
    private Long displayId;
    private String pairingCode;
    private String token;
}
