package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrivacySettingsDTO {
    private String allowRequestsFrom;
    private Boolean showConnections;
    private Boolean showMutualConnections;
}
