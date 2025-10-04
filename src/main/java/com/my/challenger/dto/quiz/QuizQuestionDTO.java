package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.MediaType;
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
    private Boolean isUserCreated;
    private Long creatorId;
    private String creatorUsername;
    private String externalId;
    private Integer usageCount;
    private QuestionType questionType;
    private String questionMediaUrl;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsed;
    
    // Media properties
    private String mediaUrl;
    private MediaType mediaType;
}