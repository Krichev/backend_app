package com.my.challenger.dto.lock;

import com.my.challenger.entity.enums.UnlockType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUnlockRequestDTO {
    private Long penaltyId;
    private UnlockType unlockType;
    private String reason;
    private String paymentType;
    private Map<String, Object> deviceInfo;
}
