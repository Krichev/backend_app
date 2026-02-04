package com.my.challenger.dto.screentime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncTimeRequest {

    @NotNull
    @Min(0)
    private Integer usedMinutes;

    @NotNull
    private String clientTimestamp;
}
