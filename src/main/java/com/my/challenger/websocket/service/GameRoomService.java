package com.my.challenger.websocket.service;

import com.my.challenger.websocket.model.GamePhase;
import com.my.challenger.websocket.model.GameRoom;
import com.my.challenger.websocket.model.RoomPlayer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class GameRoomService {

    private final Map<String, GameRoom> activeRooms = new ConcurrentHashMap<>();

    public GameRoom createRoom(Long quizSessionId, Long hostUserId) {
        String roomCode = generateRoomCode();
        GameRoom room = GameRoom.builder()
                .roomCode(roomCode)
                .quizSessionId(quizSessionId)
                .hostUserId(hostUserId)
                .currentPhase(GamePhase.LOBBY)
                .lastActivity(LocalDateTime.now())
                .build();
        activeRooms.put(roomCode, room);
        log.info("Created game room {} for session {}", roomCode, quizSessionId);
        return room;
    }

    public GameRoom getRoom(String roomCode) {
        return activeRooms.get(roomCode);
    }

    public void joinRoom(String roomCode, RoomPlayer player) {
        GameRoom room = getRoom(roomCode);
        if (room != null) {
            room.getPlayers().put(player.getUserId(), player);
            room.updateActivity();
        }
    }
    
    public void disconnectPlayer(String roomCode, Long userId) {
        GameRoom room = getRoom(roomCode);
        if (room != null) {
             RoomPlayer player = room.getPlayers().get(userId);
             if (player != null) {
                 player.setConnected(false);
                 room.updateActivity();
             }
        }
    }

    public void removeRoom(String roomCode) {
        activeRooms.remove(roomCode);
    }
    
    public void submitAnswer(String roomCode, Long userId, Long questionId, String answer) {
        GameRoom room = getRoom(roomCode);
        if (room != null && room.getCurrentPhase() == GamePhase.ANSWERING) {
            RoomPlayer player = room.getPlayers().get(userId);
            if (player != null) {
                player.setLastAnswer(answer);
                room.updateActivity();
            }
        }
    }

    @Scheduled(fixedRate = 300000) // 5 mins
    public void cleanupInactiveRooms() {
        LocalDateTime now = LocalDateTime.now();
        activeRooms.entrySet().removeIf(entry -> {
            boolean inactive = entry.getValue().getLastActivity().plusHours(2).isBefore(now);
            if (inactive) {
                log.info("Removing inactive room: {}", entry.getKey());
            }
            return inactive;
        });
    }

    private String generateRoomCode() {
        String code;
        do {
            code = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (activeRooms.containsKey(code));
        return code;
    }
}
