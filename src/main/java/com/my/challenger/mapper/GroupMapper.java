package com.my.challenger.mapper;

import com.my.challenger.dto.GroupDTO;
import com.my.challenger.entity.Group;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface GroupMapper {
    GroupMapper INSTANCE = Mappers.getMapper(GroupMapper.class);

    GroupDTO toGroupDTO(Group group);
    Group toGroup(GroupDTO groupDTO);
}
