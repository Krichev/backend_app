package com.my.challenger.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleMoveMessage {
    private String roomCode;
    private Long userId;
    private String username;
    private int pieceIndex;
    private int newRow;
    private int newCol;
}
