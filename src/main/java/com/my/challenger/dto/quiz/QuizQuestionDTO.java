// src/main/java/com/my/challenger/dto/quiz/QuizQuestionDTO.java
package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionDTO {
    private Long id;
    private String question;
    private String answer;
    private QuizDifficulty difficulty;
    private String topic;
    private String source;
    private String additionalInfo;
    private Boolean isUserCreated;
    private Long creatorId;
    private String externalId;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsed;
}

