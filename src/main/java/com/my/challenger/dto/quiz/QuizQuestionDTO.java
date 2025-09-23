// src/main/java/com/my/challenger/dto/quiz/QuizQuestionDTO.java
package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
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
    private boolean isUserCreated;
    private Long creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private Integer usageCount;

    @Builder.Default
    private QuestionType questionType = QuestionType.TEXT;

    private String questionMediaUrl;
    private String questionMediaId;
    private String questionMediaType;
    private String questionThumbnailUrl;
    private String questionMediaUrl;
    private String thumbnailUrl;
    private QuestionType questionType;

    // Add getters and setters
    public String getQuestionMediaUrl() { return questionMediaUrl; }
    public void setQuestionMediaUrl(String questionMediaUrl) { this.questionMediaUrl = questionMediaUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
}

