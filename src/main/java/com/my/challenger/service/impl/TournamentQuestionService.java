package com.my.challenger.service.impl;

import com.my.challenger.entity.quiz.Question;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.repository.QuestionRepository;
import com.my.challenger.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentQuestionService {

    private final QuestionRepository questionRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    /**
     * Add question to tournament with auto-generated sequential order
     */
    @Transactional
    public Question addQuestionToTournament(
            Integer tournamentId,
            String tournamentTitle,
            Long quizQuestionId,
            Integer points) {

        QuizQuestion quizQuestion = quizQuestionRepository.findById(quizQuestionId)
                .orElseThrow(() -> new RuntimeException("Question not found in bank"));

        if (!quizQuestion.getIsActive()) {
            throw new RuntimeException("Question is not active");
        }

        // Check if already exists
        if (questionRepository.existsByTournamentIdAndQuizQuestionId(tournamentId, quizQuestionId)) {
            throw new RuntimeException("Question already exists in this tournament");
        }

        // Get next sequential display order
        Integer nextDisplayOrder = questionRepository.getNextDisplayOrder(tournamentId);

        // Create tournament question
        Question question = Question.builder()
                .quizQuestion(quizQuestion)
                .tournamentId(tournamentId)
                .tournamentTitle(tournamentTitle)
                .displayOrder(nextDisplayOrder)
                .points(points != null ? points : 10)
                .isActive(true)
                .build();

        question = questionRepository.save(question);

        // Increment usage count
        quizQuestion.incrementUsageCount();
        quizQuestionRepository.save(quizQuestion);

        log.info("Added question {} to tournament {} at position {}",
                quizQuestionId, tournamentId, nextDisplayOrder);
        return question;
    }

    /**
     * Reorder questions in tournament
     */
    @Transactional
    public void reorderQuestions(Integer tournamentId, List<Integer> questionIds) {
        for (int i = 0; i < questionIds.size(); i++) {
            Question question = questionRepository.findById(questionIds.get(i))
                    .orElseThrow(() -> new RuntimeException("Question not found"));

            if (!question.getTournamentId().equals(tournamentId)) {
                throw new RuntimeException("Question does not belong to this tournament");
            }

            question.setDisplayOrder(i + 1);
            questionRepository.save(question);
        }

        log.info("Reordered {} questions in tournament {}", questionIds.size(), tournamentId);
    }

    /**
     * Get all questions for a tournament in proper order
     */
    @Transactional(readOnly = true)
    public List<Question> getTournamentQuestions(Integer tournamentId) {
        return questionRepository.findByTournamentIdWithQuizQuestion(tournamentId);
    }

    /**
     * Insert question at specific position
     */
    @Transactional
    public Question insertQuestionAtPosition(
            Integer tournamentId,
            String tournamentTitle,
            Long quizQuestionId,
            Integer position,
            Integer points) {

        // Shift existing questions down
        List<Question> existingQuestions = questionRepository
                .findByTournamentIdOrderByDisplayOrder(tournamentId);

        for (Question q : existingQuestions) {
            if (q.getDisplayOrder() >= position) {
                q.setDisplayOrder(q.getDisplayOrder() + 1);
                questionRepository.save(q);
            }
        }

        // Insert new question
        QuizQuestion quizQuestion = quizQuestionRepository.findById(quizQuestionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Question question = Question.builder()
                .quizQuestion(quizQuestion)
                .tournamentId(tournamentId)
                .tournamentTitle(tournamentTitle)
                .displayOrder(position)
                .points(points != null ? points : 10)
                .isActive(true)
                .build();

        return questionRepository.save(question);
    }

    /**
     * Remove question and reorder
     */
    @Transactional
    public void removeQuestionAndReorder(Integer questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Integer tournamentId = question.getTournamentId();
        Integer removedOrder = question.getDisplayOrder();

        // Soft delete
        question.setIsActive(false);
        questionRepository.save(question);

        // Shift remaining questions up
        List<Question> remainingQuestions = questionRepository
                .findByTournamentIdOrderByDisplayOrder(tournamentId);

        for (Question q : remainingQuestions) {
            if (q.getDisplayOrder() > removedOrder) {
                q.setDisplayOrder(q.getDisplayOrder() - 1);
                questionRepository.save(q);
            }
        }

        // Decrement usage count
        QuizQuestion quizQuestion = question.getQuizQuestion();
        quizQuestion.decrementUsageCount();
        quizQuestionRepository.save(quizQuestion);
    }
}