package com.my.challenger.service.puzzle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.entity.puzzle.PuzzleGame;
import com.my.challenger.entity.puzzle.PuzzleParticipant;
import com.my.challenger.entity.puzzle.PuzzlePiece;
import com.my.challenger.repository.puzzle.PuzzleGameRepository;
import com.my.challenger.repository.puzzle.PuzzleParticipantRepository;
import com.my.challenger.repository.puzzle.PuzzlePieceRepository;
import com.my.challenger.service.impl.MinioMediaStorageService;
import com.my.challenger.websocket.dto.PuzzleSpectatorMessages.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PuzzleSpectatorService {

    private final PuzzleGameRepository gameRepository;
    private final PuzzlePieceRepository pieceRepository;
    private final PuzzleParticipantRepository participantRepository;
    private final MinioMediaStorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Track active rooms for periodic broadcasts: roomCode -> gameId
    private final Map<String, Long> activePuzzleRooms = new ConcurrentHashMap<>();

    public void registerActiveRoom(String roomCode, Long gameId) {
        activePuzzleRooms.put(roomCode, gameId);
        log.info("Registered active puzzle room: {} for game {}", roomCode, gameId);
    }

    public void unregisterRoom(String roomCode) {
        activePuzzleRooms.remove(roomCode);
        log.info("Unregistered puzzle room: {}", roomCode);
    }

    @Scheduled(fixedDelay = 3000)
    public void broadcastActiveRoomSnapshots() {
        if (activePuzzleRooms.isEmpty()) return;

        for (Map.Entry<String, Long> entry : activePuzzleRooms.entrySet()) {
            String roomCode = entry.getKey();
            Long gameId = entry.getValue();
            
            try {
                broadcastSpectatorSnapshot(roomCode, gameId);
            } catch (Exception e) {
                log.error("Failed to broadcast snapshot for room {}: {}", roomCode, e.getMessage());
                // Optional: remove if game is completed
            }
        }
    }

    @Transactional(readOnly = true)
    public void broadcastSpectatorSnapshot(String roomCode, Long gameId) {
        SpectatorSnapshot snapshot = buildSpectatorSnapshot(gameId, roomCode);
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/puzzle-snapshot", snapshot);
    }

    @Transactional(readOnly = true)
    public SpectatorSnapshot buildSpectatorSnapshot(Long gameId, String roomCode) {
        PuzzleGame game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        return SpectatorSnapshot.builder()
                .state(buildStateMessage(game, roomCode))
                .players(buildPlayerSnapshots(game))
                .pieces(null) // Pieces are usually sent once on connect or via request
                .build();
    }

    public PuzzleStateMessage buildStateMessage(PuzzleGame game, String roomCode) {
        int teamCorrect = 0;
        if (game.getGameMode() == com.my.challenger.entity.enums.PuzzleGameMode.SHARED) {
            teamCorrect = game.getParticipants().stream()
                    .mapToInt(PuzzleParticipant::getPiecesPlacedCorrectly)
                    .sum();
        }

        return PuzzleStateMessage.builder()
                .roomCode(roomCode)
                .puzzleGameId(game.getId())
                .phase(game.getStatus())
                .gameMode(game.getGameMode())
                .gridRows(game.getGridRows())
                .gridCols(game.getGridCols())
                .totalPieces(game.getTotalPieces())
                .teamCorrectCount(teamCorrect)
                // Add time left if applicable
                .build();
    }

    public List<PlayerBoardSnapshot> buildPlayerSnapshots(PuzzleGame game) {
        return game.getParticipants().stream()
                .map(p -> {
                    List<PiecePlacement> board = new ArrayList<>();
                    if (p.getCurrentBoardState() != null) {
                        try {
                            board = objectMapper.readValue(p.getCurrentBoardState(), new TypeReference<List<PiecePlacement>>() {});
                        } catch (JsonProcessingException e) {
                            log.error("Error parsing board state for user {}", p.getUser().getId());
                        }
                    }
                    return PlayerBoardSnapshot.builder()
                            .userId(p.getUser().getId())
                            .username(p.getUser().getUsername())
                            .boardState(board)
                            .piecesPlacedCorrectly(p.getPiecesPlacedCorrectly())
                            .hasAnswered(p.getTextAnswer() != null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PieceMetadata> buildPieceMetadata(Long gameId) {
        List<PuzzlePiece> pieces = pieceRepository.findByPuzzleGameIdOrderByPieceIndex(gameId);
        return pieces.stream()
                .map(p -> PieceMetadata.builder()
                        .pieceIndex(p.getPieceIndex())
                        .imageUrl(storageService.getMediaUrl(p.getPieceImage()))
                        .svgClipPath(p.getSvgClipPath())
                        .edgeTop(p.getEdgeTop())
                        .edgeRight(p.getEdgeRight())
                        .edgeBottom(p.getEdgeBottom())
                        .edgeLeft(p.getEdgeLeft())
                        .widthPx(p.getWidthPx())
                        .heightPx(p.getHeightPx())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PuzzleCompletedMessage buildCompletedMessage(Long gameId) {
        PuzzleGame game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        List<PuzzleParticipant> participants = participantRepository.findByPuzzleGameIdOrderByScoreDesc(gameId);
        
        List<PlayerResult> leaderboard = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            PuzzleParticipant p = participants.get(i);
            leaderboard.add(PlayerResult.builder()
                    .username(p.getUser().getUsername())
                    .score(p.getScore())
                    .piecesPlacedCorrectly(p.getPiecesPlacedCorrectly())
                    .totalMoves(p.getTotalMoves())
                    .completionTimeMs(p.getCompletionTimeMs())
                    .rank(i + 1)
                    .build());
        }

        return PuzzleCompletedMessage.builder()
                .sourceImageUrl(storageService.getMediaUrl(game.getSourceImage()))
                .correctAnswer(game.getAnswer())
                .leaderboard(leaderboard)
                .build();
    }
}
