package com.my.challenger.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.*;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.QuizRound;
import com.my.challenger.entity.quiz.QuizSession;
import com.my.challenger.repository.ChallengeRepository;
import com.my.challenger.repository.QuizQuestionRepository;
import com.my.challenger.repository.QuizSessionRepository;
import com.my.challenger.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
@RequiredArgsConstructor
public class EnhancedQuizService extends QuizService {

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;

    /**
     * Create a quiz challenge with automatic question saving
     */
    @Transactional
    public ChallengeDTO createQuizChallenge(CreateQuizChallengeRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

        // 1. Create the quiz challenge (no VerificationDetails needed!)
        Challenge challenge = new Challenge();
        challenge.setType(ChallengeType.QUIZ);
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setCreator(creator);
        challenge.setPublic(request.getVisibility().equals("PUBLIC"));
        challenge.setVerificationMethod(VerificationMethod.QUIZ); // Set verification method
        challenge.setStartDate(request.getStartDate() != null ?
                request.getStartDate() : LocalDateTime.now());
        challenge.setEndDate(request.getEndDate());
        challenge.setFrequency(request.getFrequency());
        challenge.setStatus(ChallengeStatus.ACTIVE);

        // 2. Save quiz configuration
        if (request.getQuizConfig() != null) {
            try {
                String quizConfigJson = objectMapper.writeValueAsString(request.getQuizConfig());
                challenge.setQuizConfig(quizConfigJson);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid quiz configuration", e);
            }
        }

        Challenge savedChallenge = challengeRepository.save(challenge);

        // 3. Save user-provided questions (if any) for reusability
        if (request.getUserQuestions() != null && !request.getUserQuestions().isEmpty()) {
            saveQuizQuestions(request.getUserQuestions(), creator, savedChallenge);
        }

        // 4. Create initial task
        createInitialTask(savedChallenge, creator);

        return convertToDTO(savedChallenge, creatorId);
    }

    /**
     * Save quiz questions for reusability across different quiz sessions
     */
    @Transactional
    public List<QuizQuestionDTO> saveQuizQuestions(
            List<CreateQuizQuestionRequest> questionRequests,
            User creator,
            Challenge challenge) {

        List<QuizQuestion> savedQuestions = questionRequests.stream()
                .map(request -> {
                    // Check if question already exists to avoid duplicates
                    List<QuizQuestion> existingQuestions = quizQuestionRepository
                            .findByCreatorIdAndQuestionText(creator.getId(), request.getQuestion());

                    if (!existingQuestions.isEmpty()) {
                        log.info("Question already exists, skipping: {}", request.getQuestion());
                        return existingQuestions.get(0); // Return existing question
                    }

                    // Create new question
                    QuizQuestion question = QuizQuestion.builder()
                            .question(request.getQuestion())
                            .answer(request.getAnswer())
                            .difficulty(request.getDifficulty())
                            .topic(request.getTopic())
                            .source("USER_CREATED_FOR_CHALLENGE_" + challenge.getId())
                            .additionalInfo(request.getAdditionalInfo())
                            .isUserCreated(true)
                            .creator(creator)
                            .usageCount(0)
                            .build();

                    return quizQuestionRepository.save(question);
                })
                .collect(Collectors.toList());

        return savedQuestions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get quiz questions for a challenge (user questions + app questions if needed)
     */
    public List<QuizQuestionDTO> getQuestionsForChallenge(Long challengeId, QuizDifficulty difficulty, int count) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        // First, try to get user-created questions for this challenge
        List<QuizQuestion> userQuestions = quizQuestionRepository
                .findByCreatorIdAndSourceContaining(
                        challenge.getCreator().getId(),
                        "USER_CREATED_FOR_CHALLENGE_" + challengeId);

        List<QuizQuestionDTO> questions = userQuestions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());

        // If we don't have enough user questions, supplement with app questions
        if (questions.size() < count) {
            int remainingCount = count - questions.size();
            List<QuizQuestion> appQuestions = quizQuestionRepository
                    .findRandomQuestionsByDifficulty(difficulty.name(), remainingCount);

            List<QuizQuestionDTO> appQuestionDTOs = appQuestions.stream()
                    .map(this::convertQuestionToDTO)
                    .collect(Collectors.toList());

            questions.addAll(appQuestionDTOs);
        }

        return questions;
    }

    /**
     * Enhanced quiz session creation that automatically links to challenge
     */
    @Transactional
    public QuizSessionDTO createQuizSessionForChallenge(Long challengeId, Long hostUserId,
                                                        QuizSessionConfig sessionConfig) {

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        if (challenge.getType() != ChallengeType.QUIZ) {
            throw new IllegalArgumentException("Challenge is not a quiz type");
        }

        User hostUser = userRepository.findById(hostUserId)
                .orElseThrow(() -> new IllegalArgumentException("Host user not found"));

        // Create quiz session linked to the challenge
        QuizSession session = QuizSession.builder()
                .challenge(challenge)
                .hostUser(hostUser)
                .teamName(sessionConfig.getTeamName())
                .teamMembers(String.join(",", sessionConfig.getTeamMembers()))
                .difficulty(sessionConfig.getDifficulty())
                .roundTimeSeconds(sessionConfig.getRoundTimeSeconds())
                .totalRounds(sessionConfig.getTotalRounds())
                .enableAiHost(sessionConfig.getEnableAiHost())
                .questionSource(sessionConfig.getQuestionSource())
                .status(QuizSessionStatus.CREATED)
                .build();

        QuizSession savedSession = quizSessionRepository.save(session);

        // Get questions for this specific challenge
        List<QuizQuestionDTO> questions = getQuestionsForChallenge(
                challengeId, sessionConfig.getDifficulty(), sessionConfig.getTotalRounds());

        // Create quiz rounds
        createQuizRoundsFromQuestions(savedSession, questions);

        return convertSessionToDTO(savedSession);
    }

    private void createQuizRoundsFromQuestions(QuizSession session, List<QuizQuestionDTO> questions) {
        for (int i = 0; i < questions.size() && i < session.getTotalRounds(); i++) {
            QuizQuestionDTO questionDTO = questions.get(i);
            QuizQuestion question = quizQuestionRepository.findById(Long.valueOf(questionDTO.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Question not found"));

            QuizRound round = QuizRound.builder()
                    .quizSession(session)
                    .question(question)
                    .roundNumber(i + 1)
                    .isCorrect(false)
                    .hintUsed(false)
                    .voiceRecordingUsed(false)
                    .build();

            quizRoundRepository.save(round);
        }
    }
}