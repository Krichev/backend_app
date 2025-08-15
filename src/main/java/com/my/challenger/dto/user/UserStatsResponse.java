package com.my.challenger.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User statistics")
public class UserStatsResponse {

    @Schema(description = "Number of challenges created by user", example = "5")
    private Integer created;

    @Schema(description = "Number of challenges completed by user", example = "3")
    private Integer completed;

    @Schema(description = "Success rate percentage", example = "75.0")
    private Double success;
}