package com.my.challenger.websocket.dto;

import lombok.Data;

@Data
public class AnswerMessage {
    private Long questionId;
    private String answer;
}
