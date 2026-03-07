package com.my.challenger.dto.tv;

import com.my.challenger.entity.enums.TvDisplayStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TvDisplayStatusDTO {
    private TvDisplayStatus status;
    private String roomCode;
}
