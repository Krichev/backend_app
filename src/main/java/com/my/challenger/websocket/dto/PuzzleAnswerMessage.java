package com.my.challenger.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleAnswerMessage {
    private String roomCode;
    private Long userId;
    private String username;
    private String answer;
    private boolean correct;
    private int score;
}
