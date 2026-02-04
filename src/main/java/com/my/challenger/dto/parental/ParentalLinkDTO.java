package com.my.challenger.dto.parental;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ParentalLinkDTO {
    private Long id;
    private Long parentUserId;
    private String parentUsername;
    private Long childUserId;
    private String childUsername;
    private String status;
    private LocalDateTime verifiedAt;
    private Map<String, Boolean> permissions;
    private LocalDateTime createdAt;
}
