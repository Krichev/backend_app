package com.my.challenger.websocket.dto;

import lombok.Data;

@Data
public class ControlMessage {
    public enum Action {
        START, PAUSE, NEXT, END
    }
    private Action action;
}
