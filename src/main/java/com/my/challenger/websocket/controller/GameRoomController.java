package com.my.challenger.websocket.controller;

import com.my.challenger.security.UserPrincipal;
import com.my.challenger.websocket.dto.*;
import com.my.challenger.websocket.model.GamePhase;
import com.my.challenger.websocket.model.GameRoom;
import com.my.challenger.websocket.model.RoomPlayer;
import com.my.challenger.websocket.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameRoomController {

    private final GameRoomService gameRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{roomCode}/join")
    public void joinRoom(@DestinationVariable String roomCode, @Payload JoinRoomMessage message, SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        UserPrincipal userPrincipal = getUserPrincipal(principal);
        if (userPrincipal == null) return;

        GameRoom room = gameRoomService.getRoom(roomCode);
        if (room == null) {
            sendErrorMessage(principal.getName(), "Room not found");
            return;
        }

        RoomPlayer player = RoomPlayer.builder()
                .userId(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .role(message.getRole())
                .connected(true)
                .sessionId(headerAccessor.getSessionId())
                .score(0)
                .build();

        gameRoomService.joinRoom(roomCode, player);

        // Store roomCode in session attributes for disconnect handling
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("room_code", roomCode);
        }

        // Broadcast player list
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/players", new PlayerListMessage(room.getPlayers().values()));
        
        // Send current state to user
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/personal", 
            GameStateMessage.builder()
                .roomCode(room.getRoomCode())
                .phase(room.getCurrentPhase())
                .currentQuestionId(room.getCurrentQuestionId())
                .build()
        );
    }

    @MessageMapping("/room/{roomCode}/control")
    public void controlGame(@DestinationVariable String roomCode, @Payload ControlMessage message, Principal principal) {
        UserPrincipal userPrincipal = getUserPrincipal(principal);
        if (userPrincipal == null) return;
        
        GameRoom room = gameRoomService.getRoom(roomCode);
        if (room == null) return;

        if (!room.getHostUserId().equals(userPrincipal.getId())) {
             sendErrorMessage(principal.getName(), "Only host can control game");
             return;
        }

        switch (message.getAction()) {
            case START:
                room.setCurrentPhase(GamePhase.READING);
                break;
            case NEXT:
                // Implement next question logic
                break;
            case PAUSE:
                // Implement pause logic
                break;
            case END:
                room.setCurrentPhase(GamePhase.COMPLETED);
                break;
        }
        room.updateActivity();

        broadcastState(room);
    }

    @MessageMapping("/room/{roomCode}/answer")
    public void submitAnswer(@DestinationVariable String roomCode, @Payload AnswerMessage message, Principal principal) {
        UserPrincipal userPrincipal = getUserPrincipal(principal);
        if (userPrincipal == null) return;

        gameRoomService.submitAnswer(roomCode, userPrincipal.getId(), message.getQuestionId(), message.getAnswer());

        // Notify presenter
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/answers", 
            new AnswerBroadcastMessage(userPrincipal.getId(), userPrincipal.getUsername(), true)
        );
    }

    private void broadcastState(GameRoom room) {
        messagingTemplate.convertAndSend("/topic/room/" + room.getRoomCode() + "/state", 
            GameStateMessage.builder()
                .roomCode(room.getRoomCode())
                .phase(room.getCurrentPhase())
                .currentQuestionId(room.getCurrentQuestionId())
                .build()
        );
    }

    private void sendErrorMessage(String username, String message) {
        messagingTemplate.convertAndSendToUser(username, "/queue/personal", message); 
    }

    private UserPrincipal getUserPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            return (UserPrincipal) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        }
        return null;
    }
}
