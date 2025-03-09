package com.my.challenger.dto;

import lombok.Data;

@Data
public class RewardDTO {
    private Long id;
    private String description;
    private String type;
    private Long questId;

    // Getters and Setters
}
