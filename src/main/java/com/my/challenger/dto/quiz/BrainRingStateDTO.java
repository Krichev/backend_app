package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.BrainRingRoundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrainRingStateDTO {
    private Long currentBuzzerUserId;
    private String currentBuzzerName;
    private List<Long> lockedOutPlayers;
    private Instant answerDeadline;
    private BrainRingRoundStatus roundStatus;
    private Long winnerUserId;
}
