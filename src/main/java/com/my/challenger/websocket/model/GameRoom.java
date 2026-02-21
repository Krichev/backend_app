package com.my.challenger.websocket.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
public class GameRoom {
    private String roomCode;
    private Long hostUserId;
    private Long quizSessionId;
    private Long puzzleGameId;
    private GamePhase currentPhase;
    @Builder.Default
    private Map<Long, RoomPlayer> players = new ConcurrentHashMap<>(); // Key: userId
    private LocalDateTime lastActivity;
    
    // Additional state for game
    private Long currentQuestionId;
    private LocalDateTime timerEndTime;

    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
}
