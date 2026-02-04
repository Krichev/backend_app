package com.my.challenger.dto.parental;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class AcceptLinkRequest {
    @NotBlank
    private String verificationCode;
}
