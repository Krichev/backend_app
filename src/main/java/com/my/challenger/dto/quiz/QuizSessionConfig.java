package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuestionSource;
import com.my.challenger.entity.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;



// QuizSessionConfig.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSessionConfig {
    private String teamName;
    private List<String> teamMembers;
    private QuizDifficulty difficulty;
    private Integer roundTimeSeconds;
    private Integer totalRounds;
    private Boolean enableAiHost;
    private Boolean enableAiAnswerValidation;
    private QuestionSource questionSource;
}