package com.my.challenger.dto.lock;

import com.my.challenger.entity.enums.UnlockRequestStatus;
import com.my.challenger.entity.enums.UnlockType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnlockRequestDTO {
    private Long id;
    private Long requesterId;
    private String requesterUsername;
    private Long approverId;
    private String approverUsername;
    private Long penaltyId;
    private UnlockType unlockType;
    private UnlockRequestStatus status;
    private String paymentType;
    private Integer paymentAmount;
    private Boolean paymentFulfilled;
    private Integer bypassNumber;
    private String reason;
    private String approverMessage;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime expiresAt;
    private Map<String, Object> deviceInfo;
}
