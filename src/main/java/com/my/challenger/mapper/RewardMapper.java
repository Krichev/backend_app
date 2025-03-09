package com.my.challenger.mapper;

import com.my.challenger.dto.RewardDTO;
import com.my.challenger.entity.Reward;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RewardMapper {
    RewardMapper INSTANCE = Mappers.getMapper(RewardMapper.class);

    RewardDTO toRewardDTO(Reward reward);
    Reward toReward(RewardDTO rewardDTO);
}
