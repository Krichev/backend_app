package com.my.challenger.entity.quiz;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "questions", indexes = {
    @Index(name = "idx_tournament_id", columnList = "tournament_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tournament_id", nullable = false)
    private Integer tournamentId;

    @Column(name = "tournament_title", nullable = false, columnDefinition = "TEXT")
    private String tournamentTitle;

    @Column(name = "question_num")
    private Integer questionNum;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "authors", columnDefinition = "TEXT")
    private String authors;

    @Column(name = "sources", columnDefinition = "TEXT")
    private String sources;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "pass_criteria", columnDefinition = "TEXT")
    private String passCriteria;

    @Column(name = "notices", columnDefinition = "TEXT")
    private String notices;

    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "tournament_type", columnDefinition = "TEXT")
    private String tournamentType;

    @Column(name = "topic", columnDefinition = "TEXT")
    private String topic;

    @Column(name = "topic_num")
    private Integer topicNum;

    @Column(name = "entered_date", updatable = false)
    private LocalDateTime enteredDate;

    @PrePersist
    protected void onCreate() {
        if (enteredDate == null) {
            enteredDate = LocalDateTime.now();
        }
    }
}