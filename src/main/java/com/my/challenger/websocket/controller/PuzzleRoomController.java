package com.my.challenger.websocket.controller;

import com.my.challenger.dto.puzzle.AnswerResult;
import com.my.challenger.dto.puzzle.BoardStateUpdate;
import com.my.challenger.entity.puzzle.PuzzleGame;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.puzzle.PuzzleGameService;
import com.my.challenger.service.puzzle.PuzzleSpectatorService;
import com.my.challenger.websocket.dto.PuzzleSpectatorMessages.*;
import com.my.challenger.websocket.model.GameRoom;
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
public class PuzzleRoomController {

    private final GameRoomService gameRoomService;
    private final PuzzleGameService puzzleService;
    private final PuzzleSpectatorService spectatorService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{roomCode}/puzzle/place-piece")
    public void handlePlacePiece(@DestinationVariable String roomCode, @Payload BoardStateUpdate update, Principal principal) {
        UserPrincipal userPrincipal = getUserPrincipal(principal);
        if (userPrincipal == null) return;

        GameRoom room = gameRoomService.getRoom(roomCode);
        if (room == null || room.getPuzzleGameId() == null) return;

        puzzleService.updateBoardState(room.getPuzzleGameId(), userPrincipal.getId(), update);

        // Broadcast placement event
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/puzzle-piece", 
            PiecePlacedMessage.builder()
                .userId(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .pieceIndex(update.getPieceIndex())
                .row(update.getNewRow())
                .col(update.getNewCol())
                // .correct(...) // Optimization: could check correctness here
                .build()
        );
    }

    @MessageMapping("/room/{roomCode}/puzzle/submit-answer")
    public void handleAnswer(@DestinationVariable String roomCode, @Payload String answer, Principal principal) {
        UserPrincipal userPrincipal = getUserPrincipal(principal);
        if (userPrincipal == null) return;

        GameRoom room = gameRoomService.getRoom(roomCode);
        if (room == null || room.getPuzzleGameId() == null) return;

        AnswerResult result = puzzleService.submitAnswer(room.getPuzzleGameId(), userPrincipal.getId(), answer);

        // Broadcast answer event
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/puzzle-answer", 
            AnswerSubmittedMessage.builder()
                .userId(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .correct(result.isCorrect())
                .score(result.getScore())
                .build()
        );
    }

    @MessageMapping("/room/{roomCode}/puzzle/request-snapshot")
    public void handleSnapshotRequest(@DestinationVariable String roomCode, Principal principal) {
        GameRoom room = gameRoomService.getRoom(roomCode);
        if (room == null || room.getPuzzleGameId() == null) return;

        SpectatorSnapshot snapshot = spectatorService.buildSpectatorSnapshot(room.getPuzzleGameId(), roomCode);
        // Include piece metadata for full initial load
        snapshot.setPieces(spectatorService.buildPieceMetadata(room.getPuzzleGameId()));
        
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/personal", snapshot);
    }

    // Broadcast methods called by services
    public void broadcastPuzzleState(String roomCode, PuzzleGame game) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/puzzle-state", 
            spectatorService.buildStateMessage(game, roomCode));
    }

    public void broadcastSpectatorSnapshot(String roomCode, Long gameId) {
        spectatorService.broadcastSpectatorSnapshot(roomCode, gameId);
    }

    public void broadcastGameCompleted(String roomCode, Long gameId) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/puzzle-completed", 
            spectatorService.buildCompletedMessage(gameId));
    }

    private UserPrincipal getUserPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            return (UserPrincipal) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        }
        return null;
    }
}
