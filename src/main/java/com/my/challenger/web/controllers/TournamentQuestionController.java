// src/main/java/com/my/challenger/web/controllers/TournamentQuestionController.java
package com.my.challenger.web.controllers;

import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.quiz.Question;
import com.my.challenger.mapper.TournamentQuestionMapper;
import com.my.challenger.service.impl.TournamentQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Tournament Question Management
 * Provides CRUD operations and game-specific endpoints for tournament questions
 */
@RestController
@RequestMapping("/tournaments/{tournamentId}/questions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tournament Questions", description = "APIs for managing tournament questions")
public class TournamentQuestionController {
    
    private final TournamentQuestionService questionService;
    private final TournamentQuestionMapper questionMapper;
    
    /**
     * Get all questions for a tournament
     * Used by WWW Game Setup screen to load available questions
     */
    @GetMapping
    @Operation(summary = "Get all tournament questions", 
               description = "Retrieves all questions for a specific tournament")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Questions retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<List<TournamentQuestionSummaryDTO>> getTournamentQuestions(
            @Parameter(description = "Tournament ID", required = true)
            @PathVariable Integer tournamentId) {
        
        log.info("GET /api/tournaments/{}/questions - Fetching all questions", tournamentId);
        
        try {
            List<Question> questions = questionService.getTournamentQuestions(tournamentId);
            List<TournamentQuestionSummaryDTO> dtos = questionMapper.toSummaryDTOList(questions);
            
            log.info("Retrieved {} questions for tournament {}", dtos.size(), tournamentId);
            return ResponseEntity.ok(dtos);
            
        } catch (Exception e) {
            log.error("Error fetching questions for tournament {}: {}", tournamentId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get questions filtered by difficulty
     * NEW ENDPOINT specifically for WWW Game - returns filtered and optionally shuffled questions
     */
    @GetMapping("/by-difficulty")
    @Operation(summary = "Get questions by difficulty", 
               description = "Retrieves active questions filtered by difficulty level, optionally limited and shuffled")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Questions retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid difficulty level")
    })
    public ResponseEntity<List<TournamentQuestionSummaryDTO>> getQuestionsByDifficulty(
            @Parameter(description = "Tournament ID", required = true)
            @PathVariable Integer tournamentId,
            
            @Parameter(description = "Difficulty level: EASY, MEDIUM, or HARD", required = true)
            @RequestParam String difficulty,
            
            @Parameter(description = "Maximum number of questions to return")
            @RequestParam(defaultValue = "10") Integer limit,
            
            @Parameter(description = "Whether to shuffle results randomly")
            @RequestParam(defaultValue = "true") Boolean shuffle) {
        
        log.info("GET /api/tournaments/{}/questions/by-difficulty?difficulty={}&limit={}&shuffle={}", 
                 tournamentId, difficulty, limit, shuffle);
        
        try {
            List<TournamentQuestionSummaryDTO> questions = 
                questionService.getQuestionsByDifficulty(tournamentId, difficulty, limit, shuffle);
            
            log.info("Retrieved {} {} difficulty questions for tournament {}", 
                     questions.size(), difficulty, tournamentId);
            
            return ResponseEntity.ok(questions);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid difficulty level: {}", difficulty);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching questions by difficulty: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get single question details
     * Used when loading detailed question information for game play
     */
    @GetMapping("/{questionId}")
    @Operation(summary = "Get question details", 
               description = "Retrieves complete details for a specific question")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Question details retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Question not found")
    })
    public ResponseEntity<TournamentQuestionDetailDTO> getQuestionDetail(
            @Parameter(description = "Tournament ID", required = true)
            @PathVariable Integer tournamentId,
            
            @Parameter(description = "Question ID", required = true)
            @PathVariable Integer questionId) {
        
        log.info("GET /api/tournaments/{}/questions/{} - Fetching question details", 
                 tournamentId, questionId);
        
        try {
            Question question = questionService.getQuestionById(questionId);
            
            // Verify question belongs to this tournament
            if (!question.getTournamentId().equals(tournamentId)) {
                log.warn("Question {} does not belong to tournament {}", questionId, tournamentId);
                return ResponseEntity.notFound().build();
            }
            
            TournamentQuestionDetailDTO dto = questionMapper.toDetailDTO(question);
            return ResponseEntity.ok(dto);
            
        } catch (RuntimeException e) {
            log.error("Question not found: {}", questionId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Add question to tournament
     */
    @PostMapping
    @Operation(summary = "Add question to tournament", 
               description = "Adds a new question from the question bank to the tournament")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Question added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Quiz question not found")
    })
    public ResponseEntity<TournamentQuestionDetailDTO> addQuestion(
            @Parameter(description = "Tournament ID", required = true)
            @PathVariable Integer tournamentId,
            
            @Valid @RequestBody AddQuestionToTournamentRequest request) {
        
        log.info("POST /api/tournaments/{}/questions - Adding question {}", 
                 tournamentId, request.getQuizQuestionId());
        
        try {
            Question question = questionService.addQuestionToTournament(
                    tournamentId,
                    request.getTournamentTitle(),
                    request.getQuizQuestionId(),
                    request.getPoints()
            );
            
            TournamentQuestionDetailDTO dto = questionMapper.toDetailDTO(question);
            log.info("Successfully added question {} to tournament {}", question.getId(), tournamentId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
            
        } catch (RuntimeException e) {
            log.error("Error adding question to tournament: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Update tournament question
     */
    @PutMapping("/{questionId}")
    @Operation(summary = "Update tournament question", 
               description = "Updates tournament-specific customizations for a question")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Question updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Question not found")
    })
    public ResponseEntity<TournamentQuestionDetailDTO> updateQuestion(
            @Parameter(description = "Tournament ID", required = true)
            @PathVariable Integer tournamentId,
            
            @Parameter(description = "Question ID", required = true)
            @PathVariable Integer questionId,
            
            @Valid @RequestBody UpdateTournamentQuestionRequest request) {
        
        log.info("PUT /api/tournaments/{}/questions/{} - Updating question", 
                 tournamentId, questionId);
        
        try {
            Question question = questionService.updateTournamentQuestion(
                    questionId,
                    request.getCustomQuestion(),
                    request.getCustomAnswer(),
                    request.getPoints()
            );
            
            TournamentQuestionDetailDTO dto = questionMapper.toDetailDTO(question);
            log.info("Successfully updated question {}", questionId);
            
            return ResponseEntity.ok(dto);
            
        } catch (RuntimeException e) {
            log.error("Error updating question: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Reorder questions in tournament
     */
    @PutMapping("/reorder")
    @Operation(summary = "Reorder tournament questions", 
               description = "Updates the display order of questions in the tournament")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Questions reordered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid question order")
    })
    public ResponseEntity<Map<String, String>> reorderQuestions(
            @Parameter(description = "Tournament ID", required = true)
            @PathVariable Integer tournamentId,
            
            @Valid @RequestBody ReorderTournamentQuestionsRequest request) {
        
        log.info("PUT /api/tournaments/{}/questions/reorder - Reordering {} questions", 
                 tournamentId, request.getQuestionIds().size());
        
        try {
            questionService.reorderQuestions(tournamentId, request.getQuestionIds());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Questions reordered successfully");
            response.put("tournamentId", tournamentId.toString());
            response.put("questionsReordered", String.valueOf(request.getQuestionIds().size()));
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error reordering questions: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete question from tournament
     */
    @DeleteMapping("/{questionId}")
    @Operation(summary = "Delete tournament question", 
               description = "Removes a question from the tournament and reorders remaining questions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Question deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Question not found")
    })
    public ResponseEntity<Void> deleteQuestion(
            @Parameter(description = "Tournament ID", required = true)
            @PathVariable Integer tournamentId,
            
            @Parameter(description = "Question ID", required = true)
            @PathVariable Integer questionId) {
        
        log.info("DELETE /api/tournaments/{}/questions/{} - Deleting question", 
                 tournamentId, questionId);
        
        try {
            questionService.removeQuestionAndReorder(questionId);
            log.info("Successfully deleted question {}", questionId);
            return ResponseEntity.noContent().build();
            
        } catch (RuntimeException e) {
            log.error("Error deleting question: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get tournament question statistics
     * Used by WWW Game Setup screen to display tournament information
     */
    @GetMapping("/stats")
    @Operation(summary = "Get tournament statistics", 
               description = "Retrieves statistical information about tournament questions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<TournamentQuestionStatsDTO> getStatistics(
            @Parameter(description = "Tournament ID", required = true)
            @PathVariable Integer tournamentId) {
        
        log.info("GET /api/tournaments/{}/questions/stats - Fetching statistics", tournamentId);
        
        try {
            TournamentQuestionStatsDTO stats = questionService.getTournamentStatistics(tournamentId);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error fetching tournament statistics: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Bulk add questions to tournament
     */
    @PostMapping("/bulk")
    @Operation(summary = "Bulk add questions", 
               description = "Adds multiple questions to the tournament at once")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Questions added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<List<TournamentQuestionSummaryDTO>> bulkAddQuestions(
            @Parameter(description = "Tournament ID", required = true)
            @PathVariable Integer tournamentId,
            
            @Valid @RequestBody BulkAddQuestionsRequest request) {
        
        log.info("POST /api/tournaments/{}/questions/bulk - Adding {} questions", 
                 tournamentId, request.getQuestionsToAdd().size());
        
        try {
            // Set tournament ID in request if not already set
            if (request.getTournamentId() == null) {
                request.setTournamentId(tournamentId);
            }
            
            List<Question> questions = questionService.bulkAddQuestions(request);
            List<TournamentQuestionSummaryDTO> dtos = questionMapper.toSummaryDTOList(questions);
            
            log.info("Successfully added {} questions to tournament {}", dtos.size(), tournamentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
            
        } catch (RuntimeException e) {
            log.error("Error bulk adding questions: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Health check endpoint for tournament questions API
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if tournament questions API is available")
    public ResponseEntity<Map<String, Object>> healthCheck(
            @Parameter(description = "Tournament ID", required = true)
            @PathVariable Integer tournamentId) {
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("tournamentId", tournamentId);
        health.put("service", "TournamentQuestionController");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
}