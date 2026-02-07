package com.my.challenger.websocket.dto;

import com.my.challenger.websocket.model.PlayerRole;
import lombok.Data;

@Data
public class JoinRoomMessage {
    private String displayName; // Optional, can use username from token
    private PlayerRole role;
}
