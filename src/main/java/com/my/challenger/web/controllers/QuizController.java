package com.my.challenger.web.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.dto.MessageResponse;
import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuestionVisibility;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuizSessionStatus;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.impl.QuestionService;
import com.my.challenger.service.impl.QuizQuestionDTOEnricher;
import com.my.challenger.service.impl.QuizService;
import com.my.challenger.websocket.model.GameRoom;
import com.my.challenger.websocket.service.GameRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quiz Management", description = "Complete quiz and session management API")
public class QuizController {

    private final QuizService quizService;
    private final QuestionService questionService;
    private final QuizQuestionDTOEnricher dtoEnricher;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final GameRoomService gameRoomService;

    @Value("${app.game.join-base-url}")
    private String joinBaseUrl;


    @PostMapping(value = "/questions/with-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create question with optional media (unified endpoint)")
    public ResponseEntity<?> createQuestionWithMedia(
            @RequestPart("questionData") String questionDataJson,
            @RequestPart(value = "mediaFile", required = false) MultipartFile mediaFile,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("========== createQuestionWithMedia START ==========");
        log.info("📥 questionDataJson: {}", questionDataJson != null ? questionDataJson.substring(0, Math.min(200, questionDataJson.length())) : "null");
        log.info("📎 mediaFile is null: {}", mediaFile == null);

        if (mediaFile != null) {
            log.info("📎 mediaFile.isEmpty(): {}", mediaFile.isEmpty());
            log.info("📎 mediaFile.getOriginalFilename(): {}", mediaFile.getOriginalFilename());
            log.info("📎 mediaFile.getSize(): {}", mediaFile.getSize());
            log.info("📎 mediaFile.getContentType(): {}", mediaFile.getContentType());
            log.info("📎 mediaFile.getName(): {}", mediaFile.getName());
        } else {
            log.warn("⚠️ mediaFile is NULL - file was not received from client");
        }

        try {
            CreateQuizQuestionRequest request = objectMapper.readValue(questionDataJson, CreateQuizQuestionRequest.class);
            log.info("📄 Parsed request - question: {}, questionType: {}",
                    request.getQuestion().substring(0, Math.min(50, request.getQuestion().length())),
                    request.getQuestionType());

            if (request.getQuestion() == null || request.getQuestion().isBlank()) {
                log.error("Validation failed: question is required");
                return ResponseEntity.badRequest().build();
            }
            if (request.getAnswer() == null || request.getAnswer().isBlank()) {
                log.error("Validation failed: answer is required");
                return ResponseEntity.badRequest().build();
            }

            Long userId = ((UserPrincipal) userDetails).getId();
            log.info("👤 User ID: {}", userId);

            QuizQuestionDTO createdQuestion = questionService.createQuestionWithMedia(
                    request, mediaFile, userId);

            log.info("✅ Question created: ID={}, Type={}, MediaId={}",
                    createdQuestion.getId(),
                    createdQuestion.getQuestionType(),
                    createdQuestion.getQuestionMediaId());
            log.info("========== createQuestionWithMedia END ==========");

            return ResponseEntity.status(HttpStatus.CREATED).body(createdQuestion);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse questionData JSON: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Invalid JSON data",
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Unexpected error creating question: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @PostMapping("/questions")
    @Operation(summary = "Create a user question with visibility policy")
    public ResponseEntity<QuizQuestionDTO> createUserQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateQuizQuestionRequest request) {

        Long userId = ((UserPrincipal) userDetails).getId();
        QuizQuestionDTO question = questionService.createUserQuestion(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(question);
    }

    @GetMapping("/questions/me")
    @Operation(summary = "Get my questions with pagination")
    public ResponseEntity<Page<QuizQuestionDTO>> getMyQuestions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

         Long userId = ((UserPrincipal) userDetails).getId();
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<QuizQuestionDTO> questions = questionService.getUserQuestions(userId, pageable);
        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(questions));
    }

    @GetMapping("/questions/accessible")
    @Operation(summary = "Search accessible questions (includes public, friends, and quiz-specific)")
    public ResponseEntity<Page<QuizQuestionDTO>> searchAccessibleQuestions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Long quizId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

         Long userId = ((UserPrincipal) userDetails).getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        QuestionSearchRequest searchRequest = QuestionSearchRequest.builder()
                .keyword(keyword)
                .difficulty(difficulty != null ? QuizDifficulty.valueOf(difficulty) : null)
                .topic(topic)
                .quizId(quizId)
                .pageable(pageable)
                .build();

        Page<QuizQuestionDTO> questions = questionService.searchAccessibleQuestions(userId, searchRequest);
        return ResponseEntity.ok(dtoEnricher.enrichWithUrls(questions));
    }

    // =============================================================================
    // QUIZ SESSION MANAGEMENT ENDPOINTS
    // =============================================================================

    @PostMapping("/sessions")
    @Operation(summary = "Start a new quiz session")
    public ResponseEntity<QuizSessionDTO> startQuizSession(
            @Valid @RequestBody StartQuizSessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.startQuizSession(request, user.getId());
        log.info("User {} started quiz session: {}", user.getId(), session.getId());
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/begin")
    @Operation(summary = "Begin an existing quiz session")
    public ResponseEntity<QuizSessionDTO> beginQuizSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.beginQuizSession(sessionId, user.getId());
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get quiz session by ID")
    public ResponseEntity<QuizSessionDTO> getQuizSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.getQuizSession(sessionId, user.getId());
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/complete")
    @Operation(summary = "Complete a quiz session")
    public ResponseEntity<QuizSessionDTO> completeQuizSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        log.info("User {} completing quiz session: {}", user.getId(), sessionId);
        QuizSessionDTO session = questionService.completeQuizSession(sessionId, user.getId());
        return ResponseEntity.ok(session);
    }

    @PutMapping("/sessions/{sessionId}/pause")
    @Operation(summary = "Pause a quiz session")
    public ResponseEntity<QuizSessionDTO> pauseQuizSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody PauseQuizSessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.pauseSession(sessionId, request, user.getId());
        return ResponseEntity.ok(session);
    }

    @PutMapping("/sessions/{sessionId}/resume")
    @Operation(summary = "Resume a paused quiz session")
    public ResponseEntity<QuizSessionDTO> resumeQuizSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.resumeSession(sessionId, user.getId());
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/abandon")
    @Operation(summary = "Abandon a quiz session")
    public ResponseEntity<Void> abandonQuizSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        quizService.abandonSession(sessionId, user.getId());
        return ResponseEntity.ok().build();
    }


    @PutMapping("/questions/{questionId}/visibility")
    @Operation(summary = "Update question visibility policy")
    public ResponseEntity<QuizQuestionDTO> updateQuestionVisibility(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long questionId,
            @RequestParam QuestionVisibility visibility,
            @RequestParam(required = false) Long originalQuizId) {

         Long userId = ((UserPrincipal) userDetails).getId();
        QuizQuestionDTO question = questionService.updateQuestionVisibility(
                questionId, userId, visibility, originalQuizId);
        return ResponseEntity.ok(question);
    }

    @DeleteMapping("/questions/{questionId}")
    @Operation(summary = "Delete a user question")
    public ResponseEntity<Void> deleteUserQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long questionId) {

         Long userId = ((UserPrincipal) userDetails).getId();
        questionService.deleteUserQuestion(questionId, userId);
        return ResponseEntity.noContent().build();
    }



    @GetMapping("/sessions/{sessionId}/current-round")
    @Operation(summary = "Get current active round")
    public ResponseEntity<QuizRoundDTO> getCurrentRound(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizRoundDTO round = quizService.getCurrentRound(sessionId, user.getId());
        return ResponseEntity.ok(round);
    }

    @GetMapping("/sessions/{sessionId}/rounds")
    @Operation(summary = "Get all rounds for a quiz session")
    public ResponseEntity<List<QuizRoundDTO>> getQuizRounds(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromUserDetails(userDetails);
        List<QuizRoundDTO> rounds = questionService.getQuizRounds(sessionId, user.getId());
        return ResponseEntity.ok(rounds);
    }

    @PostMapping("/sessions/{sessionId}/rounds/{roundId}/submit")
    @Operation(summary = "Submit answer for a quiz round")
    public ResponseEntity<QuizRoundDTO> submitRoundAnswerById(
            @PathVariable Long sessionId,
            @PathVariable Long roundId,
            @Valid @RequestBody SubmitRoundAnswerByIdRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        log.info("User {} submitting answer for session {} round {}", user.getId(), sessionId, roundId);

        QuizRoundDTO result = quizService.submitRoundAnswerById(sessionId, roundId, request, user.getId());
        return ResponseEntity.ok(result);
    }

    // =============================================================================
    // SESSION CONFIGURATION
    // =============================================================================

    @PutMapping("/sessions/{sessionId}/config")
    @Operation(summary = "Update session configuration")
    public ResponseEntity<QuizSessionDTO> updateSessionConfig(
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateQuizSessionConfigRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        QuizSessionDTO session = quizService.updateSessionConfig(sessionId, request, user.getId());
        return ResponseEntity.ok(session);
    }

    @PutMapping("/sessions/{sessionId}/status")
    @Operation(summary = "Update session status")
    public ResponseEntity<MessageResponse> updateSessionStatus(
            @PathVariable Long sessionId,
            @RequestParam QuizSessionStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        quizService.updateSessionStatus(sessionId, status, user.getId());
        return ResponseEntity.ok(new MessageResponse("Session status updated successfully"));
    }

    @PostMapping("/sessions/{sessionId}/room")
    @Operation(summary = "Create a multiplayer game room for this session")
    public ResponseEntity<Map<String, Object>> createGameRoom(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromUserDetails(userDetails);
        
        // Ensure session exists and belongs to user
        quizService.getQuizSession(sessionId, user.getId()); 
        
        GameRoom room = gameRoomService.createRoom(sessionId, user.getId());
        
        String joinUrl = joinBaseUrl + "/join/" + room.getRoomCode();
        
        return ResponseEntity.ok(Map.of(
            "roomCode", room.getRoomCode(),
            "wsEndpoint", "/ws-game",
            "joinUrl", joinUrl
        ));
    }

    // =============================================================================
    // UTILITY METHODS
    // =============================================================================

    private User getUserFromUserDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }
}