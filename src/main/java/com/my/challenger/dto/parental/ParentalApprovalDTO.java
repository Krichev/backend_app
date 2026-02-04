package com.my.challenger.dto.parental;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ParentalApprovalDTO {
    private Long id;
    private Long childUserId;
    private String childUsername;
    private String approvalType;
    private Long referenceId;
    private String referenceType;
    private Map<String, Object> requestDetails;
    private String status;
    private Map<String, Object> parentResponse;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
