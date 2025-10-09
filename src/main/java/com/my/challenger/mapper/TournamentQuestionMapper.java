package com.my.challenger.mapper;

import com.my.challenger.dto.quiz.TournamentQuestionDetailDTO;
import com.my.challenger.dto.quiz.TournamentQuestionSummaryDTO;
import com.my.challenger.entity.quiz.Question;
import com.my.challenger.entity.quiz.QuizQuestion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TournamentQuestionMapper {
    
    /**
     * Map to summary DTO
     */
    public TournamentQuestionSummaryDTO toSummaryDTO(Question question) {
        if (question == null) {
            return null;
        }
        
        QuizQuestion quizQuestion = question.getQuizQuestion();
        String questionText = question.getEffectiveQuestion();
        
        return TournamentQuestionSummaryDTO.builder()
                .id(question.getId())
                .quizQuestionId(quizQuestion.getId())
                .tournamentId(question.getTournamentId())
                .tournamentTitle(question.getTournamentTitle())
                .displayOrder(question.getDisplayOrder())
                .questionPreview(truncateText(questionText, 100))
                .difficulty(quizQuestion.getDifficulty())
                .topic(quizQuestion.getTopicName())
                .questionType(quizQuestion.getQuestionType())
                .hasMedia(question.hasMedia())
                .points(question.getPoints())
                .isBonusQuestion(question.getIsBonusQuestion())
                .isMandatory(question.getIsMandatory())
                .isActive(question.getIsActive())
                .rating(question.getRating())
                .hasCustomizations(question.hasAnyCustomizations())
                .enteredDate(question.getEnteredDate())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
    
    /**
     * Map to detailed DTO
     */
    public TournamentQuestionDetailDTO toDetailDTO(Question question) {
        if (question == null) {
            return null;
        }
        
        QuizQuestion quizQuestion = question.getQuizQuestion();
        
        return TournamentQuestionDetailDTO.builder()
                .id(question.getId())
                .tournamentId(question.getTournamentId())
                .tournamentTitle(question.getTournamentTitle())
                .displayOrder(question.getDisplayOrder())
                .legacyQuestionNum(question.getLegacyQuestionNum())
                .quizQuestionId(quizQuestion.getId())
                .effectiveQuestion(question.getEffectiveQuestion())
                .effectiveAnswer(question.getEffectiveAnswer())
                .effectiveSources(question.getEffectiveSources())
                .bankQuestion(toQuizQuestionDTO(quizQuestion))
                .customQuestion(question.getCustomQuestion())
                .customAnswer(question.getCustomAnswer())
                .customSources(question.getCustomSources())
                .tournamentType(question.getTournamentType())
                .topicNum(question.getTopicNum())
                .notices(question.getNotices())
                .images(question.getImages())
                .rating(question.getRating())
                .points(question.getPoints())
                .timeLimitSeconds(question.getTimeLimitSeconds())
                .isBonusQuestion(question.getIsBonusQuestion())
                .isMandatory(question.getIsMandatory())
                .isActive(question.getIsActive())
                .hasCustomQuestion(question.hasCustomQuestion())
                .hasCustomAnswer(question.hasCustomAnswer())
                .hasCustomSources(question.hasCustomSources())
                .hasAnyCustomizations(question.hasAnyCustomizations())
                .hasMedia(question.hasMedia())
                .enteredDate(question.getEnteredDate())
                .updatedAt(question.getUpdatedAt())
                .addedBy(question.getAddedBy())
                .build();
    }
    
    /**
     * Map QuizQuestion to nested DTO
     */
    private TournamentQuestionDetailDTO.QuizQuestion toQuizQuestionDTO(QuizQuestion quizQuestion) {
        if (quizQuestion == null) {
            return null;
        }
        
        return TournamentQuestionDetailDTO.QuizQuestion.builder()
                .id(quizQuestion.getId())
                .question(quizQuestion.getQuestion())
                .answer(quizQuestion.getAnswer())
                .difficulty(quizQuestion.getDifficulty())
                .topic(quizQuestion.getTopicName())
                .source(quizQuestion.getSource())
                .authors(quizQuestion.getAuthors())
                .comments(quizQuestion.getComments())
                .passCriteria(quizQuestion.getPassCriteria())
                .additionalInfo(quizQuestion.getAdditionalInfo())
                .questionType(quizQuestion.getQuestionType())
                .questionMediaUrl(quizQuestion.getQuestionMediaUrl())
                .questionMediaId(quizQuestion.getQuestionMediaId())
                .questionMediaType(quizQuestion.getQuestionMediaType())
                .questionThumbnailUrl(quizQuestion.getQuestionThumbnailUrl())
                .usageCount(quizQuestion.getUsageCount())
                .isActive(quizQuestion.getIsActive())
                .createdAt(quizQuestion.getCreatedAt())
                .updatedAt(quizQuestion.getUpdatedAt())
                .build();
    }
    
    /**
     * Map list to summary DTOs
     */
    public List<TournamentQuestionSummaryDTO> toSummaryDTOList(List<Question> questions) {
        if (questions == null) {
            return List.of();
        }
        return questions.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Map list to detailed DTOs
     */
    public List<TournamentQuestionDetailDTO> toDetailDTOList(List<Question> questions) {
        if (questions == null) {
            return List.of();
        }
        return questions.stream()
                .map(this::toDetailDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Helper: truncate text to max length
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}