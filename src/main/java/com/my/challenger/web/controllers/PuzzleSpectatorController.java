package com.my.challenger.web.controllers;

import com.my.challenger.service.puzzle.PuzzleSpectatorService;
import com.my.challenger.websocket.dto.PuzzleSpectatorMessages.PuzzleCompletedMessage;
import com.my.challenger.websocket.dto.PuzzleSpectatorMessages.PuzzleStateMessage;
import com.my.challenger.websocket.dto.PuzzleSpectatorMessages.SpectatorSnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/puzzle/games")
@RequiredArgsConstructor
@Tag(name = "Puzzle Spectator", description = "Endpoints for TV/Spectator display of puzzle games")
public class PuzzleSpectatorController {

    private final PuzzleSpectatorService spectatorService;

    @GetMapping("/{id}/spectate")
    @Operation(summary = "Get full spectator snapshot for a puzzle game")
    public ResponseEntity<SpectatorSnapshot> getSpectatorSnapshot(
            @PathVariable Long id,
            @RequestParam(required = false) String roomCode) {
        
        SpectatorSnapshot snapshot = spectatorService.buildSpectatorSnapshot(id, roomCode);
        snapshot.setPieces(spectatorService.buildPieceMetadata(id));
        return ResponseEntity.ok(snapshot);
    }

    @GetMapping("/{id}/spectate/results")
    @Operation(summary = "Get final results for a completed puzzle game")
    public ResponseEntity<PuzzleCompletedMessage> getPuzzleResults(@PathVariable Long id) {
        return ResponseEntity.ok(spectatorService.buildCompletedMessage(id));
    }
}
