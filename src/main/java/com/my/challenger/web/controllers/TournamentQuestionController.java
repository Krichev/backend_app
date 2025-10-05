package com.my.challenger.web.controllers;

import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.quiz.Question;
import com.my.challenger.mapper.TournamentQuestionMapper;
import com.my.challenger.service.impl.TournamentQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/questions")
@RequiredArgsConstructor
public class TournamentQuestionController {
    
    private final TournamentQuestionService questionService;
    private final TournamentQuestionMapper questionMapper;
    
    /**
     * Get all questions for a tournament
     */
    @GetMapping
    public ResponseEntity<List<TournamentQuestionSummaryDTO>> getTournamentQuestions(
            @PathVariable Integer tournamentId) {
        
        List<Question> questions = questionService.getTournamentQuestions(tournamentId);
        List<TournamentQuestionSummaryDTO> dtos = questionMapper.toSummaryDTOList(questions);
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get single question details
     */
    @GetMapping("/{questionId}")
    public ResponseEntity<TournamentQuestionDetailDTO> getQuestionDetail(
            @PathVariable Integer tournamentId,
            @PathVariable Integer questionId) {
        
        Question question = questionService.getQuestionById(questionId);
        TournamentQuestionDetailDTO dto = questionMapper.toDetailDTO(question);
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Add question to tournament
     */
    @PostMapping
    public ResponseEntity<TournamentQuestionDetailDTO> addQuestion(
            @PathVariable Integer tournamentId,
            @Valid @RequestBody AddQuestionToTournamentRequest request) {
        
        Question question = questionService.addQuestionToTournament(
                tournamentId,
                request.getTournamentTitle(),
                request.getQuizQuestionId(),
                request.getPoints()
        );
        
        TournamentQuestionDetailDTO dto = questionMapper.toDetailDTO(question);
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Update question
     */
    @PutMapping("/{questionId}")
    public ResponseEntity<TournamentQuestionDetailDTO> updateQuestion(
            @PathVariable Integer tournamentId,
            @PathVariable Integer questionId,
            @Valid @RequestBody UpdateTournamentQuestionRequest request) {
        
        Question question = questionService.updateTournamentQuestion(
                questionId,
                request.getCustomQuestion(),
                request.getCustomAnswer(),
                request.getPoints()
        );
        
        TournamentQuestionDetailDTO dto = questionMapper.toDetailDTO(question);
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Reorder questions
     */
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderQuestions(
            @PathVariable Integer tournamentId,
            @Valid @RequestBody ReorderTournamentQuestionsRequest request) {
        
        questionService.reorderQuestions(tournamentId, request.getQuestionIds());
        return ResponseEntity.ok().build();
    }
    
    /**
     * Delete question
     */
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Integer tournamentId,
            @PathVariable Integer questionId) {
        
        questionService.removeQuestionAndReorder(questionId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get tournament question statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<TournamentQuestionStatsDTO> getStatistics(
            @PathVariable Integer tournamentId) {
        
        TournamentQuestionStatsDTO stats = questionService.getTournamentStatistics(tournamentId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Bulk add questions
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<TournamentQuestionSummaryDTO>> bulkAddQuestions(
            @PathVariable Integer tournamentId,
            @Valid @RequestBody BulkAddQuestionsRequest request) {
        
        List<Question> questions = questionService.bulkAddQuestions(request);
        List<TournamentQuestionSummaryDTO> dtos = questionMapper.toSummaryDTOList(questions);
        return ResponseEntity.ok(dtos);
    }
}