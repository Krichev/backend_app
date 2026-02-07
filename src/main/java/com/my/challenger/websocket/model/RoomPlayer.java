package com.my.challenger.websocket.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomPlayer {
    private Long userId;
    private String username;
    private PlayerRole role;
    private boolean connected;
    private int score;
    private String lastAnswer;
    private String sessionId; // WebSocket sessionId
}
