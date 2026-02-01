package com.my.challenger.dto.settings;

import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppSettingsRequest {

    @Pattern(regexp = "^(en|ru)$", message = "Language must be 'en' or 'ru'")
    private String language;

    @Pattern(regexp = "^(light|dark|system)$", message = "Theme must be 'light', 'dark', or 'system'")
    private String theme;

    private Boolean notificationsEnabled;
    private Boolean enableSoundEffects;
    private Boolean enableVibration;
    private Boolean enableAiAnswerValidation;
}
