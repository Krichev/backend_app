package com.my.challenger.dto;

import com.my.challenger.entity.enums.GroupType;
import com.my.challenger.entity.enums.PrivacySetting;
import com.my.challenger.entity.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupResponseDTO {
    private Long id;
    private String name;
    private String description;
    private GroupType type;
    private PrivacySetting privacy_setting;
    private int member_count;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private Long creator_id;
    private UserRole role;
}