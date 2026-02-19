package com.my.challenger.web.controllers;

import com.my.challenger.websocket.model.GameRoom;
import com.my.challenger.websocket.service.GameRoomService;
import com.my.challenger.service.impl.QuizService;
import com.my.challenger.dto.quiz.QuizSessionDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/public/rooms")
@RequiredArgsConstructor
@Tag(name = "Public Room Info", description = "Unauthenticated room information endpoints")
public class PublicRoomController {

    private final GameRoomService gameRoomService;
    private final QuizService quizService;

    @GetMapping("/{roomCode}/info")
    @Operation(summary = "Get basic info about a room by its code")
    public ResponseEntity<?> getRoomInfo(@PathVariable String roomCode) {
        GameRoom room = gameRoomService.getRoom(roomCode);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }

        // Get session info for game name/title
        try {
            // We might need a public way to get session title or just use a placeholder if not found
            // For now, let's assume we can get it if we have the ID
            // Ideally we'd have a simplified DTO
            
            return ResponseEntity.ok(Map.of(
                "roomCode", room.getRoomCode(),
                "playerCount", room.getPlayers().size(),
                "status", room.getCurrentPhase(),
                "quizSessionId", room.getQuizSessionId()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "roomCode", room.getRoomCode(),
                "playerCount", room.getPlayers().size(),
                "status", room.getCurrentPhase()
            ));
        }
    }
}
