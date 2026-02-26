package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.AssignmentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "challenge_question_assignments",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_challenge_question",
           columnNames = {"challenge_id", "question_id"}
       ),
       indexes = {
           @Index(name = "idx_cqa_challenge_id", columnList = "challenge_id"),
           @Index(name = "idx_cqa_question_id", columnList = "question_id"),
           @Index(name = "idx_cqa_challenge_order", columnList = "challenge_id, sort_order")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"challenge", "question", "assignedBy"})
@ToString(exclude = {"challenge", "question", "assignedBy"})
public class ChallengeQuestionAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 30)
    @Builder.Default
    private AssignmentType assignmentType = AssignmentType.SELECTED;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "assigned_at")
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;
}
