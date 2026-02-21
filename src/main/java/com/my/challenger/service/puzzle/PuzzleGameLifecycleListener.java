package com.my.challenger.service.puzzle;

import com.my.challenger.entity.puzzle.PuzzleGame;
import com.my.challenger.websocket.controller.PuzzleRoomController;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PuzzleGameLifecycleListener {

    private final PuzzleRoomController puzzleRoomController;
    private final PuzzleSpectatorService spectatorService;

    @EventListener
    public void handlePuzzleGameCreated(PuzzleGameCreatedEvent event) {
        log.info("Event: Puzzle Game Created {} in room {}", event.getPuzzleGameId(), event.getRoomCode());
        puzzleRoomController.broadcastPuzzleState(event.getRoomCode(), event.getGame());
    }

    @EventListener
    public void handlePuzzlePiecesGenerating(PuzzlePiecesGeneratingEvent event) {
        log.info("Event: Puzzle Pieces Generating for game {}", event.getPuzzleGameId());
        // Could broadcast specific DISTRIBUTING state if needed
    }

    @EventListener
    public void handlePuzzlePiecesReady(PuzzlePiecesReadyEvent event) {
        log.info("Event: Puzzle Pieces Ready for game {} ({} pieces)", event.getPuzzleGameId(), event.getPieceCount());
        // Pieces ready, game can move to IN_PROGRESS
    }

    @EventListener
    public void handlePuzzleGameStarted(PuzzleGameStartedEvent event) {
        log.info("Event: Puzzle Game Started {} in room {}", event.getPuzzleGameId(), event.getRoomCode());
        spectatorService.registerActiveRoom(event.getRoomCode(), event.getPuzzleGameId());
        puzzleRoomController.broadcastPuzzleState(event.getRoomCode(), event.getGame());
        puzzleRoomController.broadcastSpectatorSnapshot(event.getRoomCode(), event.getPuzzleGameId());
    }

    @EventListener
    public void handlePuzzleGameCompleted(PuzzleGameCompletedEvent event) {
        log.info("Event: Puzzle Game Completed {} in room {}", event.getPuzzleGameId(), event.getRoomCode());
        spectatorService.unregisterRoom(event.getRoomCode());
        puzzleRoomController.broadcastGameCompleted(event.getRoomCode(), event.getPuzzleGameId());
    }

    @EventListener
    public void handlePuzzleGameAbandoned(PuzzleGameAbandonedEvent event) {
        log.info("Event: Puzzle Game Abandoned {} in room {}", event.getPuzzleGameId(), event.getRoomCode());
        spectatorService.unregisterRoom(event.getRoomCode());
        // Broadcast abandonment if needed
    }

    // Event Classes
    @Getter @AllArgsConstructor
    public static class PuzzleGameCreatedEvent {
        private final Long puzzleGameId;
        private final String roomCode;
        private final PuzzleGame game;
    }

    @Getter @AllArgsConstructor
    public static class PuzzlePiecesGeneratingEvent {
        private final Long puzzleGameId;
        private final String roomCode;
    }

    @Getter @AllArgsConstructor
    public static class PuzzlePiecesReadyEvent {
        private final Long puzzleGameId;
        private final String roomCode;
        private final int pieceCount;
    }

    @Getter @AllArgsConstructor
    public static class PuzzleGameStartedEvent {
        private final Long puzzleGameId;
        private final String roomCode;
        private final PuzzleGame game;
    }

    @Getter @AllArgsConstructor
    public static class PuzzleGameCompletedEvent {
        private final Long puzzleGameId;
        private final String roomCode;
    }

    @Getter @AllArgsConstructor
    public static class PuzzleGameAbandonedEvent {
        private final Long puzzleGameId;
        private final String roomCode;
    }
}
