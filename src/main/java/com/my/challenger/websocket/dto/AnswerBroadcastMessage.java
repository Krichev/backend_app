package com.my.challenger.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnswerBroadcastMessage {
    private Long userId;
    private String username;
    private boolean answered;
}
