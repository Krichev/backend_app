package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuestionVisibility;
import com.my.challenger.entity.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSearchRequest {
    private String keyword;
    private QuizDifficulty difficulty;
    private String topic;
    private List<QuestionVisibility> visibilities;
    private Boolean onlyMyQuestions;
    private Long quizId; // For QUIZ_ONLY questions
    private Pageable pageable;
}
