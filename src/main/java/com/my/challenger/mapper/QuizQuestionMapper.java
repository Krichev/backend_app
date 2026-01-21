package com.my.challenger.mapper;

import com.my.challenger.dto.quiz.QuizQuestionDTO;
import com.my.challenger.entity.quiz.QuizQuestion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface QuizQuestionMapper {

    QuizQuestionMapper INSTANCE = Mappers.getMapper(QuizQuestionMapper.class);


    /**
     * Convert QuizQuestion entity to QuizQuestionDTO
     * All fields are mapped automatically except creator mappings
     */
    @Mapping(source = "creator.id", target = "creatorId")
    @Mapping(source = "creator.username", target = "creatorUsername")
    @Mapping(source = "topic.name", target = "topic")
    @Mapping(source = "audioReferenceMedia.id", target = "audioReferenceMediaId")
    QuizQuestionDTO toDTO(QuizQuestion entity);

    /**
     * Convert list of QuizQuestion entities to list of DTOs
     */
    List<QuizQuestionDTO> toDTOList(List<QuizQuestion> entities);

    /**
     * Convert DTO back to entity (for create/update operations)
     * Note: This won't set the creator object, only basic fields
     */
    @Mapping(target = "creator", ignore = true)
//    @Mapping(target = "tournamentQuestions", ignore = true)
    @Mapping(source = "topic", target = "topic.name")
    @Mapping(target = "audioReferenceMedia", ignore = true)
    QuizQuestion toEntity(QuizQuestionDTO dto);


}