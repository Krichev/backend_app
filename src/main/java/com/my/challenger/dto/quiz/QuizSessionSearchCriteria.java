package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizSessionStatus;

@lombok.Data
@lombok.Builder
public class QuizSessionSearchCriteria {
    private Long creatorId;
    private String questionSource;
    private QuizSessionStatus status;
    private String teamNameFilter;
    private Integer daysBack;
    private Integer limit;
}
