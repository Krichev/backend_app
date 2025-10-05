package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.quiz.Question;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.repository.QuestionRepository;
import com.my.challenger.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentQuestionService {

    private final QuestionRepository questionRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    /**
     * Get all questions for a tournament in proper order
     */
    @Transactional(readOnly = true)
    public List<Question> getTournamentQuestions(Integer tournamentId) {
        log.debug("Getting questions for tournament: {}", tournamentId);
        return questionRepository.findByTournamentIdWithQuizQuestion(tournamentId);
    }

    /**
     * Get single question by ID
     */
    @Transactional(readOnly = true)
    public Question getQuestionById(Integer questionId) {
        log.debug("Getting question by ID: {}", questionId);
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Tournament question not found with ID: " + questionId));
    }

    /**
     * Get question with quiz question eagerly loaded
     */
    @Transactional(readOnly = true)
    public Question getQuestionWithQuizQuestion(Integer questionId) {
        Question question = getQuestionById(questionId);
        // Trigger lazy loading
        question.getQuizQuestion().getQuestion();
        return question;
    }

    /**
     * Add question to tournament with auto-generated sequential order
     */
    @Transactional
    public Question addQuestionToTournament(
            Integer tournamentId,
            String tournamentTitle,
            Long quizQuestionId,
            Integer points) {

        log.info("Adding question {} to tournament {}", quizQuestionId, tournamentId);

        QuizQuestion quizQuestion = quizQuestionRepository.findById(quizQuestionId)
                .orElseThrow(() -> new RuntimeException("Question not found in bank with ID: " + quizQuestionId));

        if (!quizQuestion.getIsActive()) {
            throw new RuntimeException("Question is not active in the question bank");
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
                .isMandatory(true)
                .isBonusQuestion(false)
                .build();

        question = questionRepository.save(question);

        // Increment usage count
        quizQuestion.incrementUsageCount();
        quizQuestionRepository.save(quizQuestion);

        log.info("Successfully added question {} to tournament {} at position {}",
                quizQuestionId, tournamentId, nextDisplayOrder);

        return question;
    }

    /**
     * Add question with full customization options
     */
    @Transactional
    public Question addQuestionWithCustomization(
            Integer tournamentId,
            String tournamentTitle,
            Long quizQuestionId,
            Integer points,
            Integer timeLimitSeconds,
            Boolean isBonusQuestion,
            Boolean isMandatory,
            String customQuestion,
            String customAnswer,
            String customSources,
            String notices) {

        log.info("Adding customized question {} to tournament {}", quizQuestionId, tournamentId);

        QuizQuestion quizQuestion = quizQuestionRepository.findById(quizQuestionId)
                .orElseThrow(() -> new RuntimeException("Question not found in bank"));

        if (!quizQuestion.getIsActive()) {
            throw new RuntimeException("Question is not active");
        }

        if (questionRepository.existsByTournamentIdAndQuizQuestionId(tournamentId, quizQuestionId)) {
            throw new RuntimeException("Question already exists in this tournament");
        }

        Integer nextDisplayOrder = questionRepository.getNextDisplayOrder(tournamentId);

        Question question = Question.builder()
                .quizQuestion(quizQuestion)
                .tournamentId(tournamentId)
                .tournamentTitle(tournamentTitle)
                .displayOrder(nextDisplayOrder)
                .points(points != null ? points : 10)
                .timeLimitSeconds(timeLimitSeconds)
                .isBonusQuestion(isBonusQuestion != null ? isBonusQuestion : false)
                .isMandatory(isMandatory != null ? isMandatory : true)
                .customQuestion(customQuestion)
                .customAnswer(customAnswer)
                .customSources(customSources)
                .notices(notices)
                .isActive(true)
                .build();

        question = questionRepository.save(question);

        quizQuestion.incrementUsageCount();
        quizQuestionRepository.save(quizQuestion);

        return question;
    }

    /**
     * Update tournament-specific question customizations
     */
    @Transactional
    public Question updateTournamentQuestion(
            Integer questionId,
            String customQuestion,
            String customAnswer,
            Integer points) {

        log.info("Updating tournament question: {}", questionId);

        Question question = getQuestionById(questionId);

        if (customQuestion != null) {
            question.setCustomQuestion(customQuestion.trim().isEmpty() ? null : customQuestion);
        }
        if (customAnswer != null) {
            question.setCustomAnswer(customAnswer.trim().isEmpty() ? null : customAnswer);
        }
        if (points != null) {
            if (points < 0) {
                throw new RuntimeException("Points cannot be negative");
            }
            question.setPoints(points);
        }

        question = questionRepository.save(question);
        log.info("Successfully updated tournament question: {}", questionId);

        return question;
    }

    /**
     * Update with full customization options
     */
    @Transactional
    public Question updateTournamentQuestionFull(
            Integer questionId,
            UpdateTournamentQuestionRequest request) {

        log.info("Full update of tournament question: {}", questionId);

        Question question = getQuestionById(questionId);

        // Handle custom fields with clear flags
        if (request.getClearCustomQuestion() != null && request.getClearCustomQuestion()) {
            question.setCustomQuestion(null);
        } else if (request.getCustomQuestion() != null) {
            question.setCustomQuestion(request.getCustomQuestion().trim().isEmpty() ? null : request.getCustomQuestion());
        }

        if (request.getClearCustomAnswer() != null && request.getClearCustomAnswer()) {
            question.setCustomAnswer(null);
        } else if (request.getCustomAnswer() != null) {
            question.setCustomAnswer(request.getCustomAnswer().trim().isEmpty() ? null : request.getCustomAnswer());
        }

        if (request.getClearCustomSources() != null && request.getClearCustomSources()) {
            question.setCustomSources(null);
        } else if (request.getCustomSources() != null) {
            question.setCustomSources(request.getCustomSources().trim().isEmpty() ? null : request.getCustomSources());
        }

        // Update other fields
        if (request.getPoints() != null) {
            question.setPoints(request.getPoints());
        }
        if (request.getTimeLimitSeconds() != null) {
            question.setTimeLimitSeconds(request.getTimeLimitSeconds());
        }
        if (request.getIsBonusQuestion() != null) {
            question.setIsBonusQuestion(request.getIsBonusQuestion());
        }
        if (request.getIsMandatory() != null) {
            question.setIsMandatory(request.getIsMandatory());
        }
        if (request.getNotices() != null) {
            question.setNotices(request.getNotices());
        }
        if (request.getRating() != null) {
            question.setRating(request.getRating());
        }

        return questionRepository.save(question);
    }

    /**
     * Reorder questions in tournament
     */
    @Transactional
    public void reorderQuestions(Integer tournamentId, List<Integer> questionIds) {
        log.info("Reordering {} questions in tournament {}", questionIds.size(), tournamentId);

        if (questionIds == null || questionIds.isEmpty()) {
            throw new RuntimeException("Question IDs list cannot be empty");
        }

        // Verify all questions belong to the tournament
        for (Integer questionId : questionIds) {
            Question question = getQuestionById(questionId);
            if (!question.getTournamentId().equals(tournamentId)) {
                throw new RuntimeException(
                        String.format("Question %d does not belong to tournament %d", questionId, tournamentId));
            }
        }

        // Update display order
        for (int i = 0; i < questionIds.size(); i++) {
            Question question = getQuestionById(questionIds.get(i));
            question.setDisplayOrder(i + 1);
            questionRepository.save(question);
        }

        log.info("Successfully reordered {} questions", questionIds.size());
    }

    /**
     * Remove question from tournament and reorder remaining questions
     */
    @Transactional
    public void removeQuestionAndReorder(Integer questionId) {
        log.info("Removing question {} from tournament", questionId);

        Question question = getQuestionById(questionId);
        Integer tournamentId = question.getTournamentId();
        Integer removedOrder = question.getDisplayOrder();

        // Soft delete
        question.setIsActive(false);
        questionRepository.save(question);

        // Shift remaining questions up
        List<Question> remainingQuestions = questionRepository
                .findByTournamentIdOrderByDisplayOrder(tournamentId);

        for (Question q : remainingQuestions) {
            if (q.getDisplayOrder() > removedOrder && q.getIsActive()) {
                q.setDisplayOrder(q.getDisplayOrder() - 1);
                questionRepository.save(q);
            }
        }

        // Decrement usage count
        QuizQuestion quizQuestion = question.getQuizQuestion();
        quizQuestion.decrementUsageCount();
        quizQuestionRepository.save(quizQuestion);

        log.info("Successfully removed question {} from tournament {}", questionId, tournamentId);
    }

    /**
     * Hard delete question (permanent removal)
     */
    @Transactional
    public void deleteQuestionPermanently(Integer questionId) {
        log.warn("Permanently deleting question: {}", questionId);

        Question question = getQuestionById(questionId);
        Integer tournamentId = question.getTournamentId();
        Integer removedOrder = question.getDisplayOrder();

        // Decrement usage count first
        QuizQuestion quizQuestion = question.getQuizQuestion();
        quizQuestion.decrementUsageCount();
        quizQuestionRepository.save(quizQuestion);

        // Delete the question
        questionRepository.delete(question);

        // Reorder remaining questions
        List<Question> remainingQuestions = questionRepository
                .findByTournamentIdOrderByDisplayOrder(tournamentId);

        for (Question q : remainingQuestions) {
            if (q.getDisplayOrder() > removedOrder) {
                q.setDisplayOrder(q.getDisplayOrder() - 1);
                questionRepository.save(q);
            }
        }
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

        log.info("Inserting question {} at position {} in tournament {}",
                quizQuestionId, position, tournamentId);

        if (position < 1) {
            throw new RuntimeException("Position must be at least 1");
        }

        QuizQuestion quizQuestion = quizQuestionRepository.findById(quizQuestionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (questionRepository.existsByTournamentIdAndQuizQuestionId(tournamentId, quizQuestionId)) {
            throw new RuntimeException("Question already exists in this tournament");
        }

        // Shift existing questions down
        List<Question> existingQuestions = questionRepository
                .findByTournamentIdOrderByDisplayOrder(tournamentId);

        for (Question q : existingQuestions) {
            if (q.getDisplayOrder() >= position && q.getIsActive()) {
                q.setDisplayOrder(q.getDisplayOrder() + 1);
                questionRepository.save(q);
            }
        }

        // Insert new question
        Question question = Question.builder()
                .quizQuestion(quizQuestion)
                .tournamentId(tournamentId)
                .tournamentTitle(tournamentTitle)
                .displayOrder(position)
                .points(points != null ? points : 10)
                .isActive(true)
                .build();

        question = questionRepository.save(question);

        quizQuestion.incrementUsageCount();
        quizQuestionRepository.save(quizQuestion);

        return question;
    }

    /**
     * Bulk add questions to tournament
     */
    @Transactional
    public List<Question> bulkAddQuestions(BulkAddQuestionsRequest request) {
        log.info("Bulk adding {} questions to tournament {}",
                request.getQuestions().size(), request.getTournamentId());

        List<Question> addedQuestions = new ArrayList<>();
        Integer currentDisplayOrder = questionRepository.getNextDisplayOrder(request.getTournamentId());

        for (BulkAddQuestionsRequest.QuestionToAdd questionToAdd : request.getQuestions()) {
            QuizQuestion quizQuestion = quizQuestionRepository.findById(questionToAdd.getQuizQuestionId())
                    .orElseThrow(() -> new RuntimeException(
                            "Question not found: " + questionToAdd.getQuizQuestionId()));

            if (!quizQuestion.getIsActive()) {
                log.warn("Skipping inactive question: {}", questionToAdd.getQuizQuestionId());
                continue;
            }

            // Check if already exists
            if (questionRepository.existsByTournamentIdAndQuizQuestionId(
                    request.getTournamentId(), questionToAdd.getQuizQuestionId())) {
                log.warn("Question {} already exists in tournament, skipping",
                        questionToAdd.getQuizQuestionId());
                continue;
            }

            Question question = Question.builder()
                    .quizQuestion(quizQuestion)
                    .tournamentId(request.getTournamentId())
                    .tournamentTitle(request.getTournamentTitle())
                    .displayOrder(currentDisplayOrder++)
                    .points(questionToAdd.getPoints() != null ? questionToAdd.getPoints() : 10)
                    .timeLimitSeconds(questionToAdd.getTimeLimitSeconds())
                    .isBonusQuestion(questionToAdd.getIsBonusQuestion() != null ?
                            questionToAdd.getIsBonusQuestion() : false)
                    .customQuestion(questionToAdd.getCustomQuestion())
                    .customAnswer(questionToAdd.getCustomAnswer())
                    .isActive(true)
                    .build();

            question = questionRepository.save(question);
            addedQuestions.add(question);

            quizQuestion.incrementUsageCount();
            quizQuestionRepository.save(quizQuestion);
        }

        log.info("Successfully added {} questions to tournament {}",
                addedQuestions.size(), request.getTournamentId());

        return addedQuestions;
    }

    /**
     * Get tournament question statistics
     */
    @Transactional(readOnly = true)
    public TournamentQuestionStatsDTO getTournamentStatistics(Integer tournamentId) {
        log.debug("Calculating statistics for tournament: {}", tournamentId);

        List<Question> questions = questionRepository.findByTournamentIdWithQuizQuestion(tournamentId);

        if (questions.isEmpty()) {
            return TournamentQuestionStatsDTO.builder()
                    .tournamentId(tournamentId)
                    .totalQuestions(0)
                    .build();
        }

        // Calculate counts
        int totalQuestions = questions.size();
        int activeQuestions = (int) questions.stream().filter(Question::getIsActive).count();
        int inactiveQuestions = totalQuestions - activeQuestions;
        int bonusQuestions = (int) questions.stream()
                .filter(q -> q.getIsBonusQuestion() != null && q.getIsBonusQuestion()).count();
        int mandatoryQuestions = (int) questions.stream()
                .filter(q -> q.getIsMandatory() != null && q.getIsMandatory()).count();
        int questionsWithCustomizations = (int) questions.stream()
                .filter(Question::hasAnyCustomizations).count();
        int questionsWithMedia = (int) questions.stream()
                .filter(Question::hasMedia).count();

        // Calculate points
        int totalPoints = questions.stream()
                .filter(Question::getIsActive)
                .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                .sum();

        double averagePoints = questions.stream()
                .filter(Question::getIsActive)
                .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                .average()
                .orElse(0.0);

        Integer minPoints = questions.stream()
                .filter(Question::getIsActive)
                .map(Question::getPoints)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(0);

        Integer maxPoints = questions.stream()
                .filter(Question::getIsActive)
                .map(Question::getPoints)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        // Difficulty distribution
        Map<QuizDifficulty, Integer> difficultyDistribution = questions.stream()
                .filter(Question::getIsActive)
                .map(q -> q.getQuizQuestion().getDifficulty())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        d -> d,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Question type distribution
        Map<QuestionType, Integer> questionTypeDistribution = questions.stream()
                .filter(Question::getIsActive)
                .map(q -> q.getQuizQuestion().getQuestionType())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        t -> t,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Topic distribution
        Map<String, Integer> topicDistribution = questions.stream()
                .filter(Question::getIsActive)
                .map(q -> q.getQuizQuestion().getTopic())
                .filter(Objects::nonNull)
                .filter(t -> !t.trim().isEmpty())
                .collect(Collectors.groupingBy(
                        t -> t,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Ratings
        List<Integer> ratings = questions.stream()
                .filter(Question::getIsActive)
                .map(Question::getRating)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        double averageRating = ratings.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return TournamentQuestionStatsDTO.builder()
                .tournamentId(tournamentId)
                .tournamentTitle(questions.get(0).getTournamentTitle())
                .totalQuestions(totalQuestions)
                .activeQuestions(activeQuestions)
                .inactiveQuestions(inactiveQuestions)
                .bonusQuestions(bonusQuestions)
                .mandatoryQuestions(mandatoryQuestions)
                .questionsWithCustomizations(questionsWithCustomizations)
                .questionsWithMedia(questionsWithMedia)
                .totalPoints(totalPoints)
                .averagePoints(averagePoints)
                .minPoints(minPoints)
                .maxPoints(maxPoints)
                .difficultyDistribution(difficultyDistribution)
                .questionTypeDistribution(questionTypeDistribution)
                .topicDistribution(topicDistribution)
                .averageRating(averageRating)
                .questionsWithRating(ratings.size())
                .build();
    }

    /**
     * Copy questions from one tournament to another
     */
    @Transactional
    public List<Question> copyQuestions(CopyQuestionsRequest request) {
        log.info("Copying questions from tournament {} to {}",
                request.getSourceTournamentId(), request.getTargetTournamentId());

        if (request.getSourceTournamentId().equals(request.getTargetTournamentId())) {
            throw new RuntimeException("Source and target tournaments must be different");
        }

        List<Question> sourceQuestions;

        if (request.getCopyAll() != null && request.getCopyAll()) {
            sourceQuestions = questionRepository.findByTournamentIdOrderByDisplayOrder(
                    request.getSourceTournamentId());
        } else if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
            sourceQuestions = request.getQuestionIds().stream()
                    .map(this::getQuestionById)
                    .collect(Collectors.toList());
        } else {
            throw new RuntimeException("Must specify either copyAll=true or provide questionIds");
        }

        List<Question> copiedQuestions = new ArrayList<>();
        Integer startOrder = questionRepository.getNextDisplayOrder(request.getTargetTournamentId());

        for (int i = 0; i < sourceQuestions.size(); i++) {
            Question source = sourceQuestions.get(i);

            // Check if already exists
            if (questionRepository.existsByTournamentIdAndQuizQuestionId(
                    request.getTargetTournamentId(), source.getQuizQuestion().getId())) {
                log.warn("Question {} already exists in target tournament, skipping",
                        source.getQuizQuestion().getId());
                continue;
            }

            Integer displayOrder = request.getPreserveOrder() != null && request.getPreserveOrder() ?
                    source.getDisplayOrder() : startOrder + i;

            Question copy = Question.builder()
                    .quizQuestion(source.getQuizQuestion())
                    .tournamentId(request.getTargetTournamentId())
                    .tournamentTitle(request.getTargetTournamentTitle())
                    .displayOrder(displayOrder)
                    .points(source.getPoints())
                    .timeLimitSeconds(source.getTimeLimitSeconds())
                    .isBonusQuestion(source.getIsBonusQuestion())
                    .isMandatory(source.getIsMandatory())
                    .tournamentType(source.getTournamentType())
                    .isActive(true)
                    .build();

            // Copy customizations if requested
            if (request.getIncludeCustomizations() != null && request.getIncludeCustomizations()) {
                copy.setCustomQuestion(source.getCustomQuestion());
                copy.setCustomAnswer(source.getCustomAnswer());
                copy.setCustomSources(source.getCustomSources());
                copy.setNotices(source.getNotices());
            }

            copy = questionRepository.save(copy);
            copiedQuestions.add(copy);

            // Increment usage count
            QuizQuestion quizQuestion = source.getQuizQuestion();
            quizQuestion.incrementUsageCount();
            quizQuestionRepository.save(quizQuestion);
        }

        log.info("Successfully copied {} questions to tournament {}",
                copiedQuestions.size(), request.getTargetTournamentId());

        return copiedQuestions;
    }

    /**
     * Validate tournament questions
     */
    @Transactional(readOnly = true)
    public List<TournamentQuestionValidationDTO> validateTournamentQuestions(Integer tournamentId) {
        log.debug("Validating questions for tournament: {}", tournamentId);

        List<Question> questions = questionRepository.findByTournamentIdWithQuizQuestion(tournamentId);
        List<TournamentQuestionValidationDTO> validations = new ArrayList<>();

        for (Question question : questions) {
            TournamentQuestionValidationDTO validation = TournamentQuestionValidationDTO.builder()
                    .questionId(question.getId())
                    .isValid(true)
                    .build();

            // Validate display order
            if (question.getDisplayOrder() == null || question.getDisplayOrder() < 1) {
                validation.addError("displayOrder", "INVALID_ORDER",
                        "Display order must be a positive integer");
            }

            // Validate points
            if (question.getPoints() == null || question.getPoints() < 0) {
                validation.addError("points", "INVALID_POINTS",
                        "Points must be non-negative");
            }

            // Validate effective question/answer
            String effectiveQuestion = question.getEffectiveQuestion();
            String effectiveAnswer = question.getEffectiveAnswer();

            if (effectiveQuestion == null || effectiveQuestion.trim().isEmpty()) {
                validation.addError("question", "EMPTY_QUESTION",
                        "Question text cannot be empty");
            }

            if (effectiveAnswer == null || effectiveAnswer.trim().isEmpty()) {
                validation.addError("answer", "EMPTY_ANSWER",
                        "Answer text cannot be empty");
            }

            // Validate quiz question reference
            if (question.getQuizQuestion() == null) {
                validation.addError("quizQuestion", "MISSING_REFERENCE",
                        "Question must reference a quiz question");
            } else if (!question.getQuizQuestion().getIsActive()) {
                validation.addWarning("quizQuestion", "INACTIVE_REFERENCE",
                        "Referenced quiz question is inactive");
            }

            // Warning for very long customizations
            if (question.getCustomQuestion() != null &&
                    question.getCustomQuestion().length() > 2000) {
                validation.addWarning("customQuestion", "LONG_TEXT",
                        "Custom question text is very long");
            }

            validations.add(validation);
        }

        return validations;
    }

    /**
     * Get available questions from bank for a tournament (not already added)
     */
    @Transactional(readOnly = true)
    public List<QuizQuestion> getAvailableQuestionsForTournament(
            Integer tournamentId,
            Pageable pageable) {

        log.debug("Getting available questions for tournament: {}", tournamentId);
        return quizQuestionRepository.findQuestionsNotInTournament(tournamentId);
    }

    /**
     * Search tournament questions
     */
    @Transactional(readOnly = true)
    public List<Question> searchTournamentQuestions(TournamentQuestionSearchRequest request) {
        log.debug("Searching tournament questions with filters");

        List<Question> questions = questionRepository
                .findByTournamentIdWithQuizQuestion(request.getTournamentId());

        return questions.stream()
                .filter(q -> matchesSearchCriteria(q, request))
                .collect(Collectors.toList());
    }

    private boolean matchesSearchCriteria(Question question, TournamentQuestionSearchRequest request) {
        if (request.getDifficulty() != null &&
                !request.getDifficulty().equals(question.getQuizQuestion().getDifficulty())) {
            return false;
        }

        if (request.getTopic() != null &&
                !request.getTopic().equals(question.getQuizQuestion().getTopic())) {
            return false;
        }

        if (request.getQuestionType() != null &&
                !request.getQuestionType().equals(question.getQuizQuestion().getQuestionType())) {
            return false;
        }

        if (request.getHasMedia() != null &&
                request.getHasMedia() != question.hasMedia()) {
            return false;
        }

        if (request.getIsBonusQuestion() != null &&
                !request.getIsBonusQuestion().equals(question.getIsBonusQuestion())) {
            return false;
        }

        if (request.getHasCustomizations() != null &&
                request.getHasCustomizations() != question.hasAnyCustomizations()) {
            return false;
        }

        if (request.getIsActive() != null &&
                !request.getIsActive().equals(question.getIsActive())) {
            return false;
        }

        if (request.getSearchText() != null && !request.getSearchText().trim().isEmpty()) {
            String searchText = request.getSearchText().toLowerCase();
            String questionText = question.getEffectiveQuestion().toLowerCase();
            String answerText = question.getEffectiveAnswer().toLowerCase();

            if (!questionText.contains(searchText) && !answerText.contains(searchText)) {
                return false;
            }
        }

        return true;
    }
}