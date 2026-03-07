package com.my.challenger.dto.tv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TvDisplayClaimDTO {
    private boolean success;
    private String roomCode;
    private Long displayId;
}
