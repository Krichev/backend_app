//// src/main/java/com/my/challenger/util/builders/QuizSessionBuilder.java
//package com.my.challenger.util.builders;
//
//import com.my.challenger.entity.User;
//import com.my.challenger.entity.challenge.Challenge;
//import com.my.challenger.entity.enums.QuizDifficulty;
//import com.my.challenger.entity.enums.QuizSessionStatus;
//import com.my.challenger.entity.quiz.QuizSession;
//
//import java.time.LocalDateTime;
//
///**
// * Builder pattern for QuizSession entity
// */
//public class QuizSessionBuilder {
//    private Challenge challenge;
//    private User hostUser;
//    private String teamName;
//    private String teamMembers;
//    private QuizDifficulty difficulty;
//    private Integer roundTimeSeconds;
//    private Integer totalRounds;
//    private Integer currentRound;
//    private QuizSessionStatus status;
//    private Boolean enableAiHost;
//    private String questionSource;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//    private LocalDateTime startedAt;
//    private LocalDateTime completedAt;
//
//    public QuizSessionBuilder challenge(Challenge challenge) {
//        this.challenge = challenge;
//        return this;
//    }
//
//    public QuizSessionBuilder hostUser(User hostUser) {
//        this.hostUser = hostUser;
//        return this;
//    }
//
//    public QuizSessionBuilder teamName(String teamName) {
//        this.teamName = teamName;
//        return this;
//    }
//
//    public QuizSessionBuilder teamMembers(String teamMembers) {
//        this.teamMembers = teamMembers;
//        return this;
//    }
//
//    public QuizSessionBuilder difficulty(QuizDifficulty difficulty) {
//        this.difficulty = difficulty;
//        return this;
//    }
//
//    public QuizSessionBuilder roundTimeSeconds(Integer roundTimeSeconds) {
//        this.roundTimeSeconds = roundTimeSeconds;
//        return this;
//    }
//
//    public QuizSessionBuilder totalRounds(Integer totalRounds) {
//        this.totalRounds = totalRounds;
//        return this;
//    }
//
//    /**
//     * This method was missing and causing compilation errors
//     */
//    public QuizSessionBuilder currentRound(Integer currentRound) {
//        this.currentRound = currentRound;
//        return this;
//    }
//
//    public QuizSessionBuilder status(QuizSessionStatus status) {
//        this.status = status;
//        return this;
//    }
//
//    public QuizSessionBuilder enableAiHost(Boolean enableAiHost) {
//        this.enableAiHost = enableAiHost;
//        return this;
//    }
//
//    public QuizSessionBuilder questionSource(String questionSource) {
//        this.questionSource = questionSource;
//        return this;
//    }
//
//    public QuizSessionBuilder createdAt(LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//        return this;
//    }
//
//    public QuizSessionBuilder updatedAt(LocalDateTime updatedAt) {
//        this.updatedAt = updatedAt;
//        return this;
//    }
//
//    public QuizSessionBuilder startedAt(LocalDateTime startedAt) {
//        this.startedAt = startedAt;
//        return this;
//    }
//
//    public QuizSessionBuilder completedAt(LocalDateTime completedAt) {
//        this.completedAt = completedAt;
//        return this;
//    }
//
//    /**
//     * Build the QuizSession object
//     */
//    public QuizSession build() {
//        QuizSession session = new QuizSession();
//        session.setChallenge(challenge);
//        session.setHostUser(hostUser);
//        session.setTeamName(teamName);
//        session.setTeamMembers(teamMembers);
//        session.setDifficulty(difficulty);
//        session.setRoundTimeSeconds(roundTimeSeconds);
//        session.setTotalRounds(totalRounds);
//        session.setCurrentRound(currentRound != null ? currentRound : 0);
//        session.setStatus(status != null ? status : QuizSessionStatus.CREATED);
//        session.setEnableAiHost(enableAiHost != null ? enableAiHost : false);
//        session.setQuestionSource(questionSource);
//
//        // Set timestamps
//        LocalDateTime now = LocalDateTime.now();
//        session.setCreatedAt(createdAt != null ? createdAt : now);
//        session.setUpdatedAt(updatedAt != null ? updatedAt : now);
//
//        if (startedAt != null) {
//            session.setStartedAt(startedAt);
//        }
//        if (completedAt != null) {
//            session.setCompletedAt(completedAt);
//        }
//
//        return session;
//    }
//
//    /**
//     * Create a new builder instance
//     */
//    public static QuizSessionBuilder builder() {
//        return new QuizSessionBuilder();
//    }
//}