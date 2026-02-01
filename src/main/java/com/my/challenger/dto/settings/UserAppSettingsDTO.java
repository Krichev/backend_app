package com.my.challenger.dto.settings;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAppSettingsDTO {
    private Long id;
    private Long userId;
    private String language;
    private String theme;
    private Boolean notificationsEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean enableSoundEffects;
    private Boolean enableVibration;
    private Boolean enableAiAnswerValidation;
}
