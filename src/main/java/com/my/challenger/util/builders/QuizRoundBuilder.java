//// src/main/java/com/my/challenger/util/builders/QuizRoundBuilder.java
//package com.my.challenger.util.builders;
//
//import com.my.challenger.entity.quiz.QuizQuestion;
//import com.my.challenger.entity.quiz.QuizRound;
//import com.my.challenger.entity.quiz.QuizSession;
//
//import java.time.LocalDateTime;
//
///**
// * Builder pattern for QuizRound entity
// */
//public class QuizRoundBuilder {
//    private QuizSession quizSession;
//    private QuizQuestion question;
//    private Integer roundNumber;
//    private String teamAnswer;
//    private String playerWhoAnswered;
//    private Boolean isCorrect;
//    private Boolean hintUsed;
//    private Boolean voiceRecordingUsed;
//    private Integer mediaInteractionCount;
//    private LocalDateTime questionStartedAt;
//    private LocalDateTime discussionStartedAt;
//    private LocalDateTime answerSubmittedAt;
//    private Integer discussionDurationSeconds;
//    private Integer totalTimeSeconds;
//    private String discussionNotes;
//    private String voiceTranscription;
//
//    public QuizRoundBuilder quizSession(QuizSession quizSession) {
//        this.quizSession = quizSession;
//        return this;
//    }
//
//    public QuizRoundBuilder question(QuizQuestion question) {
//        this.question = question;
//        return this;
//    }
//
//    public QuizRoundBuilder roundNumber(Integer roundNumber) {
//        this.roundNumber = roundNumber;
//        return this;
//    }
//
//    public QuizRoundBuilder teamAnswer(String teamAnswer) {
//        this.teamAnswer = teamAnswer;
//        return this;
//    }
//
//    public QuizRoundBuilder playerWhoAnswered(String playerWhoAnswered) {
//        this.playerWhoAnswered = playerWhoAnswered;
//        return this;
//    }
//
//    public QuizRoundBuilder isCorrect(Boolean isCorrect) {
//        this.isCorrect = isCorrect;
//        return this;
//    }
//
//    public QuizRoundBuilder hintUsed(Boolean hintUsed) {
//        this.hintUsed = hintUsed;
//        return this;
//    }
//
//    public QuizRoundBuilder voiceRecordingUsed(Boolean voiceRecordingUsed) {
//        this.voiceRecordingUsed = voiceRecordingUsed;
//        return this;
//    }
//
//    /**
//     * This method was missing and causing compilation errors
//     */
//    public QuizRoundBuilder mediaInteractionCount(Integer mediaInteractionCount) {
//        this.mediaInteractionCount = mediaInteractionCount;
//        return this;
//    }
//
//    public QuizRoundBuilder questionStartedAt(LocalDateTime questionStartedAt) {
//        this.questionStartedAt = questionStartedAt;
//        return this;
//    }
//
//    public QuizRoundBuilder discussionStartedAt(LocalDateTime discussionStartedAt) {
//        this.discussionStartedAt = discussionStartedAt;
//        return this;
//    }
//
//    public QuizRoundBuilder answerSubmittedAt(LocalDateTime answerSubmittedAt) {
//        this.answerSubmittedAt = answerSubmittedAt;
//        return this;
//    }
//
//    public QuizRoundBuilder discussionDurationSeconds(Integer discussionDurationSeconds) {
//        this.discussionDurationSeconds = discussionDurationSeconds;
//        return this;
//    }
//
//    public QuizRoundBuilder totalTimeSeconds(Integer totalTimeSeconds) {
//        this.totalTimeSeconds = totalTimeSeconds;
//        return this;
//    }
//
//    public QuizRoundBuilder discussionNotes(String discussionNotes) {
//        this.discussionNotes = discussionNotes;
//        return this;
//    }
//
//    public QuizRoundBuilder voiceTranscription(String voiceTranscription) {
//        this.voiceTranscription = voiceTranscription;
//        return this;
//    }
//
//    /**
//     * Build the QuizRound object
//     */
//    public QuizRound build() {
//        QuizRound round = new QuizRound();
//        round.setQuizSession(quizSession);
//        round.setQuestion(question);
//        round.setRoundNumber(roundNumber);
//        round.setTeamAnswer(teamAnswer);
//        round.setPlayerWhoAnswered(playerWhoAnswered);
//        round.setIsCorrect(isCorrect != null ? isCorrect : false);
//        round.setHintUsed(hintUsed != null ? hintUsed : false);
//        round.setVoiceRecordingUsed(voiceRecordingUsed != null ? voiceRecordingUsed : false);
//        round.setMediaInteractionCount(mediaInteractionCount != null ? mediaInteractionCount : 0);
//        round.setQuestionStartedAt(questionStartedAt);
//        round.setDiscussionStartedAt(discussionStartedAt);
//        round.setAnswerSubmittedAt(answerSubmittedAt);
//        round.setDiscussionDurationSeconds(discussionDurationSeconds);
//        round.setTotalTimeSeconds(totalTimeSeconds);
//        round.setDiscussionNotes(discussionNotes);
//        round.setVoiceTranscription(voiceTranscription);
//
//        return round;
//    }
//
//    /**
//     * Create a new builder instance
//     */
//    public static QuizRoundBuilder builder() {
//        return new QuizRoundBuilder();
//    }
//}