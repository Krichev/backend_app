package com.my.challenger.mapper;

import com.my.challenger.dto.QuestDTO;
import com.my.challenger.entity.Quest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface QuestMapper {
    QuestMapper INSTANCE = Mappers.getMapper(QuestMapper.class);

    QuestDTO toQuestDTO(Quest quest);
    Quest toQuest(QuestDTO questDTO);
}
