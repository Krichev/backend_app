package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuestionAccessType;
import com.my.challenger.entity.quiz.QuizQuestion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_access_log",
    indexes = {
        @Index(name = "idx_question_access_log_question", columnList = "question_id"),
        @Index(name = "idx_question_access_log_user", columnList = "accessed_by_user_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"question", "accessedByUser"})
@ToString(exclude = {"question", "accessedByUser"})
public class QuestionAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_question_access_log_question"))
    private QuizQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accessed_by_user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_question_access_log_user"))
    private User accessedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 50)
    private QuestionAccessType accessType;

    @CreationTimestamp
    @Column(name = "accessed_at", nullable = false, updatable = false)
    private LocalDateTime accessedAt;
}