package com.my.challenger.mapper;

import com.my.challenger.dto.RewardUserDTO;
import com.my.challenger.entity.RewardUser;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RewardUserMapper {
    RewardUserMapper INSTANCE = Mappers.getMapper(RewardUserMapper.class);

    RewardUserDTO toRewardUserDTO(RewardUser rewardUser);
    RewardUser toRewardUser(RewardUserDTO rewardUserDTO);
}
