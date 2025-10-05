package com.my.challenger.migration;

import com.my.challenger.entity.quiz.Question;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionBankMigrationService {

    private final JdbcTemplate jdbcTemplate;
    private final QuizQuestionRepository quizQuestionRepository;

    /**
     * MAIN MIGRATION METHOD with smart ordering
     */
    @Transactional
    public MigrationResult migrateQuestionsToBank() {
        log.info("========================================");
        log.info("Starting Question Bank Migration");
        log.info("========================================");

        MigrationResult result = new MigrationResult();

        try {
            // Step 1: Analyze question_num distribution
            log.info("Step 1: Analyzing question_num distribution...");
            analyzeQuestionNumDistribution(result);

            // Step 2: Load all old questions
            log.info("Step 2: Loading old questions...");
            List<OldQuestion> oldQuestions = loadOldQuestions();
            result.totalOldQuestions = oldQuestions.size();
            log.info("Loaded {} old questions", result.totalOldQuestions);

            // Step 3: Group by tournament for proper ordering
            log.info("Step 3: Grouping questions by tournament...");
            Map<Integer, List<OldQuestion>> questionsByTournament = oldQuestions.stream()
                    .collect(Collectors.groupingBy(q -> q.tournamentId));
            log.info("Found {} unique tournaments", questionsByTournament.size());

            // Step 4: Create question bank with deduplication
            log.info("Step 4: Creating question bank with deduplication...");
            Map<String, QuizQuestion> questionBankMap = createQuestionBank(oldQuestions, result);
            log.info("Created {} unique questions in bank", result.uniqueQuestionsCreated);
            log.info("Detected {} duplicate questions", result.duplicatesFound);

            // Step 5: Create tournament questions with proper ordering
            log.info("Step 5: Creating tournament question instances with proper ordering...");
            createTournamentQuestionsWithOrdering(questionsByTournament, questionBankMap, result);
            log.info("Created {} tournament question instances", result.tournamentQuestionsCreated);

            // Step 6: Verification
            log.info("Step 6: Verifying migration...");
            verifyMigration(result);

            log.info("========================================");
            log.info("Migration completed successfully!");
            log.info("Summary:");
            log.info("  - Old questions: {}", result.totalOldQuestions);
            log.info("  - Unique bank questions: {}", result.uniqueQuestionsCreated);
            log.info("  - Duplicates detected: {}", result.duplicatesFound);
            log.info("  - Tournament instances: {}", result.tournamentQuestionsCreated);
            log.info("  - Questions with zero/null num: {}", result.questionsWithBadNum);
            log.info("========================================");

        } catch (Exception e) {
            log.error("Migration failed!", e);
            result.errors.add("Migration failed: " + e.getMessage());
            throw new RuntimeException("Migration failed", e);
        }

        return result;
    }

    /**
     * Analyze question_num distribution
     */
    private void analyzeQuestionNumDistribution(MigrationResult result) {
        String sql = """
            SELECT 
                COUNT(*) as total,
                COUNT(CASE WHEN question_num = 0 THEN 1 END) as zero_nums,
                COUNT(CASE WHEN question_num IS NULL THEN 1 END) as null_nums,
                COUNT(CASE WHEN question_num > 0 THEN 1 END) as valid_nums
            FROM questions_old
            """;

        jdbcTemplate.query(sql, rs -> {
            int total = rs.getInt("total");
            int zeroNums = rs.getInt("zero_nums");
            int nullNums = rs.getInt("null_nums");
            int validNums = rs.getInt("valid_nums");

            result.questionsWithBadNum = zeroNums + nullNums;

            double badPercentage = (double) (zeroNums + nullNums) / total * 100;

            log.info("Question number analysis:");
            log.info("  - Total questions: {}", total);
            log.info("  - Zero question_num: {} ({:.1f}%)", zeroNums, (double) zeroNums / total * 100);
            log.info("  - Null question_num: {} ({:.1f}%)", nullNums, (double) nullNums / total * 100);
            log.info("  - Valid question_num: {} ({:.1f}%)", validNums, (double) validNums / total * 100);
            log.info("  ⚠️  Will auto-generate sequential order for {:.1f}% of questions", badPercentage);
        });
    }

    /**
     * Load all questions from old table
     */
    private List<OldQuestion> loadOldQuestions() {
        String sql = """
            SELECT 
                id, tournament_id, tournament_title, question_num,
                question, answer, authors, sources, comments,
                pass_criteria, notices, images, rating,
                tournament_type, topic, topic_num, entered_date
            FROM questions_old
            ORDER BY tournament_id, 
                     CASE WHEN question_num > 0 THEN question_num ELSE 999999 END,
                     id
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            OldQuestion q = new OldQuestion();
            q.id = rs.getInt("id");
            q.tournamentId = rs.getInt("tournament_id");
            q.tournamentTitle = rs.getString("tournament_title");
            q.questionNum = (Integer) rs.getObject("question_num");
            q.question = rs.getString("question");
            q.answer = rs.getString("answer");
            q.authors = rs.getString("authors");
            q.sources = rs.getString("sources");
            q.comments = rs.getString("comments");
            q.passCriteria = rs.getString("pass_criteria");
            q.notices = rs.getString("notices");
            q.images = rs.getString("images");
            q.rating = (Integer) rs.getObject("rating");
            q.tournamentType = rs.getString("tournament_type");
            q.topic = rs.getString("topic");
            q.topicNum = (Integer) rs.getObject("topic_num");
            q.enteredDate = rs.getObject("entered_date", LocalDateTime.class);
            return q;
        });
    }

    /**
     * Create unique question bank with deduplication
     */
    private Map<String, QuizQuestion> createQuestionBank(
            List<OldQuestion> oldQuestions,
            MigrationResult result) {

        Map<String, QuizQuestion> bankMap = new HashMap<>();
        int batchSize = 100;
        int count = 0;

        for (OldQuestion oldQ : oldQuestions) {
            // Create unique key for deduplication
            String uniqueKey = createQuestionKey(oldQ.question, oldQ.answer);

            if (bankMap.containsKey(uniqueKey)) {
                // Duplicate found
                result.duplicatesFound++;
                continue;
            }

            // Create new QuizQuestion
            QuizQuestion quizQuestion = QuizQuestion.builder()
                    .question(cleanText(oldQ.question))
                    .answer(cleanText(oldQ.answer))
                    .difficulty(inferDifficulty(oldQ))
                    .topic(oldQ.topic)
                    .source(oldQ.sources)
                    .authors(oldQ.authors)
                    .comments(oldQ.comments)
                    .passCriteria(oldQ.passCriteria)
                    .additionalInfo(oldQ.notices)
                    .questionType(inferQuestionType(oldQ))
                    .isUserCreated(false)
                    .isActive(true)
                    .usageCount(0)
                    .legacyQuestionId(oldQ.id)
                    .build();

            // Handle images/media
            if (oldQ.images != null && !oldQ.images.trim().isEmpty()) {
                quizQuestion.setQuestionMediaUrl(oldQ.images);
                quizQuestion.setQuestionType(QuestionType.IMAGE);
            }

            quizQuestion = quizQuestionRepository.save(quizQuestion);
            bankMap.put(uniqueKey, quizQuestion);
            result.uniqueQuestionsCreated++;

            count++;
            if (count % batchSize == 0) {
                log.info("Created {} question bank entries...", count);
                quizQuestionRepository.flush();
            }
        }

        quizQuestionRepository.flush();
        return bankMap;
    }

    /**
     * Create tournament questions with proper sequential ordering
     */
    private void createTournamentQuestionsWithOrdering(
            Map<Integer, List<OldQuestion>> questionsByTournament,
            Map<String, QuizQuestion> bankMap,
            MigrationResult result) {

        String insertSql = """
            INSERT INTO tournament_questions (
                quiz_question_id, tournament_id, tournament_title,
                display_order, legacy_question_num,
                tournament_type, topic_num, notices, images, rating,
                points, is_active, entered_date, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        int totalCreated = 0;
        int tournamentsProcessed = 0;

        for (Map.Entry<Integer, List<OldQuestion>> entry : questionsByTournament.entrySet()) {
            Integer tournamentId = entry.getKey();
            List<OldQuestion> tournamentQuestions = entry.getValue();

            // Sort questions intelligently:
            // 1. If question_num > 0, use it
            // 2. Otherwise, sort by ID to maintain insertion order
            tournamentQuestions.sort((q1, q2) -> {
                int num1 = (q1.questionNum != null && q1.questionNum > 0) ? q1.questionNum : 999999;
                int num2 = (q2.questionNum != null && q2.questionNum > 0) ? q2.questionNum : 999999;

                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
                return Integer.compare(q1.id, q2.id);
            });

            // Create tournament questions with sequential display_order
            int displayOrder = 1;
            for (OldQuestion oldQ : tournamentQuestions) {
                String uniqueKey = createQuestionKey(oldQ.question, oldQ.answer);
                QuizQuestion quizQuestion = bankMap.get(uniqueKey);

                if (quizQuestion == null) {
                    log.warn("No quiz question found for old question ID: {}", oldQ.id);
                    result.errors.add("Missing quiz question for old ID: " + oldQ.id);
                    continue;
                }

                // Insert tournament question with proper sequential order
                jdbcTemplate.update(insertSql,
                        quizQuestion.getId(),
                        oldQ.tournamentId,
                        oldQ.tournamentTitle,
                        displayOrder++, // Auto-generated sequential order
                        oldQ.questionNum, // Keep old number for reference
                        oldQ.tournamentType,
                        oldQ.topicNum,
                        oldQ.notices,
                        oldQ.images,
                        oldQ.rating,
                        10, // default points
                        true, // is_active
                        oldQ.enteredDate,
                        LocalDateTime.now()
                );

                // Increment usage count
                quizQuestion.incrementUsageCount();

                totalCreated++;
            }

            tournamentsProcessed++;
            if (tournamentsProcessed % 10 == 0) {
                log.info("Processed {} tournaments, created {} tournament questions...",
                        tournamentsProcessed, totalCreated);
                quizQuestionRepository.flush();
            }
        }

        result.tournamentQuestionsCreated = totalCreated;
        quizQuestionRepository.flush();
    }

    /**
     * Verify migration results
     */
    private void verifyMigration(MigrationResult result) {
        Integer oldCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM questions_old", Integer.class);
        Integer newCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tournament_questions", Integer.class);
        Integer bankCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quiz_questions WHERE legacy_question_id IS NOT NULL",
                Integer.class);

        log.info("Verification:");
        log.info("  Old questions: {}", oldCount);
        log.info("  New tournament questions: {}", newCount);
        log.info("  Question bank entries: {}", bankCount);

        if (!oldCount.equals(newCount)) {
            String error = String.format(
                    "Count mismatch! Old: %d, New: %d", oldCount, newCount);
            result.errors.add(error);
            throw new RuntimeException(error);
        }

        // Verify ordering
        String orderCheckSql = """
            SELECT tournament_id, COUNT(*) as question_count,
                   MIN(display_order) as min_order,
                   MAX(display_order) as max_order
            FROM tournament_questions
            GROUP BY tournament_id
            HAVING MIN(display_order) != 1 
                OR MAX(display_order) != COUNT(*)
            """;

        List<String> orderIssues = jdbcTemplate.query(orderCheckSql, (rs, rowNum) ->
                String.format("Tournament %d has ordering issues: count=%d, min=%d, max=%d",
                        rs.getInt("tournament_id"),
                        rs.getInt("question_count"),
                        rs.getInt("min_order"),
                        rs.getInt("max_order"))
        );

        if (!orderIssues.isEmpty()) {
            log.warn("Found {} tournaments with ordering issues", orderIssues.size());
            result.errors.addAll(orderIssues);
        }

        result.verificationPassed = orderIssues.isEmpty();
    }

    // =============== HELPER METHODS ===============

    private String createQuestionKey(String question, String answer) {
        return normalizeText(question) + "|" + normalizeText(answer);
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.trim().toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-zа-я0-9 ]", "");
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.trim();
    }

    private QuizDifficulty inferDifficulty(OldQuestion q) {
        if (q.answer == null) return QuizDifficulty.MEDIUM;

        int length = q.answer.length();
        if (length < 20) return QuizDifficulty.EASY;
        if (length < 100) return QuizDifficulty.MEDIUM;
        return QuizDifficulty.HARD;
    }

    private QuestionType inferQuestionType(OldQuestion q) {
        if (q.images != null && !q.images.trim().isEmpty()) {
            return QuestionType.IMAGE;
        }
        return QuestionType.TEXT;
    }

    // =============== DATA CLASSES ===============

    private static class OldQuestion {
        Integer id;
        Integer tournamentId;
        String tournamentTitle;
        Integer questionNum;
        String question;
        String answer;
        String authors;
        String sources;
        String comments;
        String passCriteria;
        String notices;
        String images;
        Integer rating;
        String tournamentType;
        String topic;
        Integer topicNum;
        LocalDateTime enteredDate;
    }

    @lombok.Data
    public static class MigrationResult {
        int totalOldQuestions = 0;
        int uniqueQuestionsCreated = 0;
        int duplicatesFound = 0;
        int tournamentQuestionsCreated = 0;
        int questionsWithBadNum = 0;
        boolean verificationPassed = false;
        List<String> errors = new ArrayList<>();
    }
}