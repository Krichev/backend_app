package com.my.challenger.dto.quiz;

@lombok.Data
@lombok.Builder
public class UpdateQuizSessionConfigRequest {
    private Integer roundTimeSeconds;
    private Boolean enableAiHost;
    private String teamName;
    private String teamMembers;
}