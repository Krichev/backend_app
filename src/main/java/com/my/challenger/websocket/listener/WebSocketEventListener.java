package com.my.challenger.websocket.listener;

import com.my.challenger.security.UserPrincipal;
import com.my.challenger.websocket.dto.PlayerListMessage;
import com.my.challenger.websocket.model.GameRoom;
import com.my.challenger.websocket.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final GameRoomService gameRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Object roomCodeObj = headerAccessor.getSessionAttributes().get("room_code");
        String roomCode = roomCodeObj instanceof String ? (String) roomCodeObj : null;
        Principal principal = event.getUser();

        if (roomCode != null && principal != null) {
            UserPrincipal userPrincipal = getUserPrincipal(principal);
            if (userPrincipal != null) {
                Long userId = userPrincipal.getId();
                log.info("User {} disconnected from room {}", userPrincipal.getUsername(), roomCode);
                
                gameRoomService.disconnectPlayer(roomCode, userId);
                
                GameRoom room = gameRoomService.getRoom(roomCode);
                if (room != null) {
                    messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/players", 
                        new PlayerListMessage(room.getPlayers().values())
                    );
                }
            }
        }
    }

    private UserPrincipal getUserPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            return (UserPrincipal) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        }
        return null;
    }
}
