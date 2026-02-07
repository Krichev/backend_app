package com.my.challenger.websocket.dto;

import com.my.challenger.websocket.model.GamePhase;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GameStateMessage {
    private String roomCode;
    private GamePhase phase;
    private Long currentQuestionId;
    private LocalDateTime timerEndTime;
}
