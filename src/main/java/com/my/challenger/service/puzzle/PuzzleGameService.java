package com.my.challenger.service.puzzle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.dto.puzzle.*;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.PuzzleGameMode;
import com.my.challenger.entity.enums.PuzzleSessionStatus;
import com.my.challenger.entity.puzzle.PuzzleGame;
import com.my.challenger.entity.puzzle.PuzzleParticipant;
import com.my.challenger.entity.puzzle.PuzzlePiece;
import com.my.challenger.repository.ChallengeRepository;
import com.my.challenger.repository.MediaFileRepository;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.repository.puzzle.PuzzleGameRepository;
import com.my.challenger.repository.puzzle.PuzzleParticipantRepository;
import com.my.challenger.repository.puzzle.PuzzlePieceRepository;
import com.my.challenger.service.impl.MinioMediaStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PuzzleGameService {

    private final PuzzleGameRepository puzzleGameRepository;
    private final PuzzlePieceRepository puzzlePieceRepository;
    private final PuzzleParticipantRepository participantRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final MediaFileRepository mediaFileRepository;
    private final JigsawSplitterService splitterService;
    private final MinioMediaStorageService storageService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PuzzleGameDTO createPuzzleGame(CreatePuzzleGameRequest request, Long creatorId) {
        Challenge challenge = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));
        
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));
        
        MediaFile sourceImage = mediaFileRepository.findById(request.getSourceImageMediaId())
                .orElseThrow(() -> new IllegalArgumentException("Source image not found"));

        String answerAliasesJson = null;
        if (request.getAnswerAliases() != null) {
            try {
                answerAliasesJson = objectMapper.writeValueAsString(request.getAnswerAliases());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize answer aliases", e);
            }
        }

        String roomCode = generateRoomCode();

        PuzzleGame game = PuzzleGame.builder()
                .challenge(challenge)
                .sourceImage(sourceImage)
                .gameMode(request.getGameMode())
                .roomCode(roomCode)
                .gridRows(request.getGridRows())
                .gridCols(request.getGridCols())
                .totalPieces(request.getGridRows() * request.getGridCols())
                .answer(request.getAnswer())
                .answerAliases(answerAliasesJson)
                .difficulty(request.getDifficulty())
                .timeLimitSeconds(request.getTimeLimitSeconds())
                .hintText(request.getHintText())
                .hintAvailableAfterSeconds(request.getHintAvailableAfterSeconds())
                .enableAiValidation(request.isEnableAiValidation())
                .status(PuzzleSessionStatus.CREATED)
                .creator(creator)
                .build();

        game = puzzleGameRepository.save(game);

        eventPublisher.publishEvent(new PuzzleGameLifecycleListener.PuzzleGameCreatedEvent(
                game.getId(), roomCode, game));

        return PuzzleDtoMapper.toDTO(game);
    }

    private String generateRoomCode() {
        return RandomStringUtils.randomAlphanumeric(6).toUpperCase();
    }

    @Transactional
    public void generatePuzzlePieces(Long gameId) {
        PuzzleGame game = puzzleGameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
        
        if (game.getStatus() != PuzzleSessionStatus.CREATED) {
            throw new IllegalStateException("Pieces can only be generated for games in CREATED status");
        }

        game.setStatus(PuzzleSessionStatus.DISTRIBUTING);
        puzzleGameRepository.save(game);

        eventPublisher.publishEvent(new PuzzleGameLifecycleListener.PuzzlePiecesGeneratingEvent(
                gameId, game.getRoomCode()));

        splitterService.splitImage(game).thenAccept(pieces -> {
            updateGamePieces(gameId, pieces);
        }).exceptionally(ex -> {
            handleSplitFailure(gameId, ex);
            return null;
        });
    }

    @Transactional
    protected void updateGamePieces(Long gameId, List<PuzzlePiece> pieces) {
        PuzzleGame game = puzzleGameRepository.findById(gameId).orElse(null);
        if (game != null) {
            puzzlePieceRepository.saveAll(pieces);
            game.setStatus(PuzzleSessionStatus.IN_PROGRESS);
            puzzleGameRepository.save(game);
            log.info("Game {} updated with {} pieces and status IN_PROGRESS", gameId, pieces.size());

            eventPublisher.publishEvent(new PuzzleGameLifecycleListener.PuzzlePiecesReadyEvent(
                    gameId, game.getRoomCode(), pieces.size()));
        }
    }

    @Transactional
    protected void handleSplitFailure(Long gameId, Throwable ex) {
        log.error("Failed to generate pieces for game {}", gameId, ex);
        PuzzleGame game = puzzleGameRepository.findById(gameId).orElse(null);
        if (game != null) {
            game.setStatus(PuzzleSessionStatus.CREATED);
            puzzleGameRepository.save(game);
        }
    }

    @Transactional
    public PuzzleParticipantDTO joinPuzzleGame(Long gameId, Long userId) {
        PuzzleGame game = puzzleGameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<PuzzleParticipant> existing = participantRepository.findByPuzzleGameIdAndUserId(gameId, userId);
        if (existing.isPresent()) {
            return PuzzleDtoMapper.toDTO(existing.get());
        }

        PuzzleParticipant participant = PuzzleParticipant.builder()
                .puzzleGame(game)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .build();

        participant = participantRepository.save(participant);
        return PuzzleDtoMapper.toDTO(participant);
    }

    @Transactional
    public void startGame(Long gameId, Long hostUserId) {
        PuzzleGame game = puzzleGameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        if (!game.getCreator().getId().equals(hostUserId)) {
            throw new IllegalStateException("Only the creator can start the game");
        }

        if (game.getStatus() != PuzzleSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game must be in IN_PROGRESS status to start (pieces must be generated)");
        }

        game.setStartedAt(LocalDateTime.now());
        puzzleGameRepository.save(game);

        if (game.getGameMode() == PuzzleGameMode.SHARED) {
            distributePieces(gameId);
        }

        eventPublisher.publishEvent(new PuzzleGameLifecycleListener.PuzzleGameStartedEvent(
                gameId, game.getRoomCode(), game));
    }

    @Transactional
    public void distributePieces(Long gameId) {
        PuzzleGame game = puzzleGameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        List<PuzzlePiece> pieces = puzzlePieceRepository.findByPuzzleGameIdOrderByPieceIndex(gameId);
        List<PuzzleParticipant> participants = game.getParticipants();

        if (participants.isEmpty()) {
            log.warn("No participants to distribute pieces for game {}", gameId);
            return;
        }

        Collections.shuffle(pieces);
        
        int participantCount = participants.size();
        int piecesPerPlayer = pieces.size() / participantCount;
        int remainder = pieces.size() % participantCount;

        int currentIndex = 0;
        for (int i = 0; i < participantCount; i++) {
            int count = piecesPerPlayer + (i < remainder ? 1 : 0);
            List<Long> assignedIds = new ArrayList<>();
            for (int j = 0; j < count; j++) {
                assignedIds.add(pieces.get(currentIndex++).getId());
            }
            
            try {
                participants.get(i).setAssignedPieceIds(objectMapper.writeValueAsString(assignedIds));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize assigned piece IDs", e);
            }
        }
        
        participantRepository.saveAll(participants);
    }

    @Transactional(readOnly = true)
    public List<PuzzlePieceDTO> getPlayerPieces(Long gameId, Long userId) {
        PuzzleGame game = puzzleGameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        PuzzleParticipant participant = participantRepository.findByPuzzleGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        List<PuzzlePiece> allPieces = puzzlePieceRepository.findByPuzzleGameIdOrderByPieceIndex(gameId);
        
        if (game.getGameMode() == PuzzleGameMode.INDIVIDUAL) {
            // In Individual mode, player has all pieces
            return allPieces.stream()
                    .map(p -> PuzzleDtoMapper.toDTO(p, storageService.getMediaUrl(p.getPieceImage()), true))
                    .collect(Collectors.toList());
        } else {
            // In Shared mode, player only has assigned pieces
            if (participant.getAssignedPieceIds() == null) {
                return Collections.emptyList();
            }
            
            try {
                List<Long> assignedIds = objectMapper.readValue(participant.getAssignedPieceIds(), new TypeReference<List<Long>>() {});
                Set<Long> idSet = new HashSet<>(assignedIds);
                
                return allPieces.stream()
                        .filter(p -> idSet.contains(p.getId()))
                        .map(p -> PuzzleDtoMapper.toDTO(p, storageService.getMediaUrl(p.getPieceImage()), false))
                        .collect(Collectors.toList());
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize assigned piece IDs", e);
                return Collections.emptyList();
            }
        }
    }

    @Transactional
    public void updateBoardState(Long gameId, Long userId, BoardStateUpdate update) {
        PuzzleParticipant participant = participantRepository.findByPuzzleGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        PuzzleGame game = participant.getPuzzleGame();
        if (game.getCompletedAt() != null) {
            throw new IllegalStateException("Game already completed");
        }

        List<PiecePlacement> boardState;
        try {
            if (participant.getCurrentBoardState() == null) {
                boardState = new ArrayList<>();
            } else {
                boardState = objectMapper.readValue(participant.getCurrentBoardState(), new TypeReference<List<PiecePlacement>>() {});
            }

            // Update or add piece placement
            Optional<PiecePlacement> existing = boardState.stream()
                    .filter(p -> p.getPieceIndex() == update.getPieceIndex())
                    .findFirst();
            
            if (existing.isPresent()) {
                existing.get().setCurrentRow(update.getNewRow());
                existing.get().setCurrentCol(update.getNewCol());
            } else {
                boardState.add(new PiecePlacement(update.getPieceIndex(), update.getNewRow(), update.getNewCol()));
            }

            participant.setCurrentBoardState(objectMapper.writeValueAsString(boardState));
            participant.setTotalMoves(participant.getTotalMoves() + 1);
            
            // Calculate correctly placed pieces
            List<PuzzlePiece> pieces = puzzlePieceRepository.findByPuzzleGameIdOrderByPieceIndex(gameId);
            Map<Integer, PuzzlePiece> pieceMap = pieces.stream()
                    .collect(Collectors.toMap(PuzzlePiece::getPieceIndex, p -> p));
            
            int correctCount = 0;
            for (PiecePlacement p : boardState) {
                PuzzlePiece target = pieceMap.get(p.getPieceIndex());
                if (target != null && target.getGridRow() == p.getCurrentRow() && target.getGridCol() == p.getCurrentCol()) {
                    correctCount++;
                }
            }
            participant.setPiecesPlacedCorrectly(correctCount);
            
            participantRepository.save(participant);

        } catch (JsonProcessingException e) {
            log.error("Failed to process board state JSON", e);
        }
    }

    @Transactional
    public AnswerResult submitAnswer(Long gameId, Long userId, String answer) {
        PuzzleParticipant participant = participantRepository.findByPuzzleGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        PuzzleGame game = participant.getPuzzleGame();
        if (participant.getTextAnswer() != null) {
            throw new IllegalStateException("Answer already submitted");
        }

        participant.setTextAnswer(answer);
        participant.setAnswerSubmittedAt(LocalDateTime.now());

        boolean correct = checkAnswer(game, answer);
        participant.setAnswerCorrect(correct);

        int score = 0;
        if (correct) {
            score = calculateScore(game, participant);
            participant.setScore(score);
            participant.setCompletionTimeMs(java.time.Duration.between(game.getStartedAt(), LocalDateTime.now()).toMillis());
        }

        participantRepository.save(participant);

        // Check if all participants answered (for Mode A) or if game should end
        if (checkGameCompletion(game)) {
            eventPublisher.publishEvent(new PuzzleGameLifecycleListener.PuzzleGameCompletedEvent(
                    gameId, game.getRoomCode()));
        }

        return AnswerResult.builder()
                .correct(correct)
                .message(correct ? "Correct!" : "Try again")
                .score(score)
                .rank(calculateRank(gameId, userId))
                .build();
    }

    private boolean checkAnswer(PuzzleGame game, String answer) {
        if (answer == null || game.getAnswer() == null) return false;
        
        String normalizedInput = answer.trim().toLowerCase();
        String normalizedCorrect = game.getAnswer().trim().toLowerCase();
        
        if (normalizedInput.equals(normalizedCorrect)) return true;
        
        if (game.getAnswerAliases() != null) {
            try {
                List<String> aliases = objectMapper.readValue(game.getAnswerAliases(), new TypeReference<List<String>>() {});
                for (String alias : aliases) {
                    if (normalizedInput.equals(alias.trim().toLowerCase())) return true;
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse answer aliases", e);
            }
        }
        
        return false;
    }

    private int calculateScore(PuzzleGame game, PuzzleParticipant participant) {
        int baseScore = 1000;
        // Deduction for time if applicable
        long elapsedSeconds = java.time.Duration.between(game.getStartedAt(), LocalDateTime.now()).getSeconds();
        int timeDeduction = (int) (elapsedSeconds * 2);
        
        int finalScore = Math.max(100, baseScore - timeDeduction);
        
        // Bonus for accuracy if Mode B
        if (game.getGameMode() == PuzzleGameMode.INDIVIDUAL) {
            double accuracy = (double) participant.getPiecesPlacedCorrectly() / game.getTotalPieces();
            finalScore += (int) (accuracy * 500);
        }
        
        return finalScore;
    }

    private int calculateRank(Long gameId, Long userId) {
        List<PuzzleParticipant> participants = participantRepository.findByPuzzleGameIdOrderByScoreDesc(gameId);
        for (int i = 0; i < participants.size(); i++) {
            if (participants.get(i).getUser().getId().equals(userId)) {
                return i + 1;
            }
        }
        return 0;
    }

    private boolean checkGameCompletion(PuzzleGame game) {
        List<PuzzleParticipant> participants = game.getParticipants();
        boolean allAnswered = participants.stream().allMatch(p -> p.getTextAnswer() != null);
        
        if (allAnswered) {
            game.setCompletedAt(LocalDateTime.now());
            game.setStatus(PuzzleSessionStatus.COMPLETED);
            puzzleGameRepository.save(game);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public PuzzleGameStatusDTO getGameStatus(Long gameId) {
        PuzzleGame game = puzzleGameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
        return PuzzleDtoMapper.toStatusDTO(game);
    }
    
    @Transactional
    public void abandonGame(Long gameId, Long userId) {
        PuzzleGame game = puzzleGameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
        
        if (!game.getCreator().getId().equals(userId)) {
            throw new IllegalStateException("Only the creator can abandon the game");
        }
        
        game.setStatus(PuzzleSessionStatus.ABANDONED);
        game.setCompletedAt(LocalDateTime.now());
        puzzleGameRepository.save(game);

        eventPublisher.publishEvent(new PuzzleGameLifecycleListener.PuzzleGameAbandonedEvent(
                gameId, game.getRoomCode()));
    }

    @Transactional(readOnly = true)
    public SpectatorViewDTO getSpectatorView(Long gameId) {
        PuzzleGame game = puzzleGameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
        
        List<SpectatorPlayerState> players = game.getParticipants().stream()
                .map(p -> {
                    List<PiecePlacement> boardState = Collections.emptyList();
                    if (p.getCurrentBoardState() != null) {
                        try {
                            boardState = objectMapper.readValue(p.getCurrentBoardState(), new TypeReference<List<PiecePlacement>>() {});
                        } catch (JsonProcessingException e) {
                            log.error("Failed to parse board state", e);
                        }
                    }
                    return SpectatorPlayerState.builder()
                            .username(p.getUser().getUsername())
                            .boardState(boardState)
                            .hasAnswered(p.getTextAnswer() != null)
                            .build();
                })
                .collect(Collectors.toList());
        
        return SpectatorViewDTO.builder()
                .game(PuzzleDtoMapper.toDTO(game))
                .players(players)
                .build();
    }
}
