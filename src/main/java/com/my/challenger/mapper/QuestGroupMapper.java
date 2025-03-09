package com.my.challenger.mapper;

import com.my.challenger.dto.QuestGroupDTO;
import com.my.challenger.entity.QuestGroup;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface QuestGroupMapper {
    QuestGroupMapper INSTANCE = Mappers.getMapper(QuestGroupMapper.class);

    QuestGroupDTO toQuestGroupDTO(QuestGroup questGroup);
    QuestGroup toQuestGroup(QuestGroupDTO questGroupDTO);
}
