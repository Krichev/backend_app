package com.my.challenger.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class QuestDTO {
    private Long id;
    private String title;
    private String description;
    private String type;
    private String visibility;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long creatorId;

    // Getters and Setters
}
