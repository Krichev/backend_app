package com.my.challenger.dto.screentime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigureBudgetRequest {

    @NotNull
    @Min(30)
    @Max(480)
    private Integer dailyBudgetMinutes;

    private String timezone;
}
