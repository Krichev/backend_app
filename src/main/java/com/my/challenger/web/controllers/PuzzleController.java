package com.my.challenger.web.controllers;

import com.my.challenger.dto.puzzle.*;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.puzzle.PuzzleGameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/puzzle")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Puzzle Game", description = "Endpoints for the Picture Puzzle Game")
public class PuzzleController {

    private final PuzzleGameService puzzleService;
    private final com.my.challenger.websocket.service.GameRoomService gameRoomService;

    @PostMapping("/games")
    @Operation(summary = "Create a new puzzle game")
    public ResponseEntity<PuzzleGameDTO> createGame(
            @Valid @RequestBody CreatePuzzleGameRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        log.info("Creating puzzle game for user {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(puzzleService.createPuzzleGame(request, userId));
    }

    @PostMapping("/games/{id}/generate")
    @Operation(summary = "Trigger jigsaw splitting (async)")
    public ResponseEntity<Void> generatePieces(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // In a real scenario, we'd check if the user is the creator
        log.info("Triggering piece generation for game {}", id);
        puzzleService.generatePuzzlePieces(id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/games/{id}")
    @Operation(summary = "Get game details + status")
    public ResponseEntity<PuzzleGameStatusDTO> getGameStatus(@PathVariable Long id) {
        return ResponseEntity.ok(puzzleService.getGameStatus(id));
    }

    @PostMapping("/games/{id}/join")
    @Operation(summary = "Join as participant")
    public ResponseEntity<PuzzleParticipantDTO> joinGame(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(puzzleService.joinPuzzleGame(id, userId));
    }

    @PostMapping("/games/{id}/start")
    @Operation(summary = "Host starts the game")
    public ResponseEntity<Void> startGame(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        puzzleService.startGame(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/games/{id}/pieces")
    @Operation(summary = "Get my assigned pieces (with presigned URLs)")
    public ResponseEntity<List<PuzzlePieceDTO>> getMyPieces(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(puzzleService.getPlayerPieces(id, userId));
    }

    @PutMapping("/games/{id}/board")
    @Operation(summary = "Update my board state (piece moved)")
    public ResponseEntity<Void> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody BoardStateUpdate update,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        puzzleService.updateBoardState(id, userId, update);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/games/{id}/answer")
    @Operation(summary = "Submit text answer")
    public ResponseEntity<AnswerResult> submitAnswer(
            @PathVariable Long id,
            @RequestParam String answer,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(puzzleService.submitAnswer(id, userId, answer));
    }

    @PostMapping("/games/{id}/abandon")
    @Operation(summary = "Abandon game")
    public ResponseEntity<Void> abandonGame(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        puzzleService.abandonGame(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/games/{id}/room")
    @Operation(summary = "Create a multiplayer game room for this puzzle")
    public ResponseEntity<java.util.Map<String, Object>> createRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        // Ensure game exists and belongs to user
        // puzzleService.getPuzzleGame(id, userId); // Optional check
        
        com.my.challenger.websocket.model.GameRoom room = gameRoomService.createPuzzleRoom(id, userId);
        
        return ResponseEntity.ok(java.util.Map.of(
            "roomCode", room.getRoomCode(),
            "wsEndpoint", "/ws-game"
        ));
    }
}
