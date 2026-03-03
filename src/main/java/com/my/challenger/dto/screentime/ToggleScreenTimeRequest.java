package com.my.challenger.dto.screentime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleScreenTimeRequest {
    @NotNull
    private Boolean enabled;
}
