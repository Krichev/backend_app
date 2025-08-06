// QuizSessionDetailDTO.java
package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSessionDetailDTO {
    private QuizSessionDTO session;
    private List<QuizRoundDTO> rounds;
    private Integer totalScore;
    private Double accuracy;
}