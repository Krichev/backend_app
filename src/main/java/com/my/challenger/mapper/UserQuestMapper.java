package com.my.challenger.mapper;

import com.my.challenger.dto.UserQuestDTO;
import com.my.challenger.entity.UserQuest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserQuestMapper {
    UserQuestMapper INSTANCE = Mappers.getMapper(UserQuestMapper.class);

    UserQuestDTO toUserQuestDTO(UserQuest userQuest);
    UserQuest toUserQuest(UserQuestDTO userQuestDTO);
}
