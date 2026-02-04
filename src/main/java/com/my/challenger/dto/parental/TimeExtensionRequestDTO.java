package com.my.challenger.dto.parental;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TimeExtensionRequestDTO {
    private Long id;
    private Long childUserId;
    private String childUsername;
    private Integer minutesRequested;
    private String reason;
    private String status;
    private Integer minutesGranted;
    private String parentMessage;
    private LocalDateTime respondedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
