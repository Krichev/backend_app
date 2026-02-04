package com.my.challenger.dto.parental;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class LinkChildRequest {
    @NotNull
    private Long childUserId;
}
