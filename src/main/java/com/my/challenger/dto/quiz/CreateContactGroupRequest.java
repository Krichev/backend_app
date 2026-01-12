package com.my.challenger.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContactGroupRequest {
    @NotBlank(message = "Group name is required")
    private String name;
    private String color;
    private String icon;
}
