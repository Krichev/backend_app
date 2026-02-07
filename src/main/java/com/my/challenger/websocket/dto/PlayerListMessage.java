package com.my.challenger.websocket.dto;

import com.my.challenger.websocket.model.RoomPlayer;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Collection;

@Data
@AllArgsConstructor
public class PlayerListMessage {
    private Collection<RoomPlayer> players;
}
