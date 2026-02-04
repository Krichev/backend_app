package com.my.challenger.dto.parental;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class ApproveExtensionRequest {
    @NotNull
    @Min(1)
    private Integer minutesToGrant;
    private String message;
}
