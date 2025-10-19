package com.my.challenger.service.impl;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuestionAccessType;
import com.my.challenger.entity.enums.QuestionVisibility;
import com.my.challenger.entity.quiz.QuestionAccessLog;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.repository.QuestionAccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionAccessService {

    private final QuestionAccessLogRepository accessLogRepository;
    private final UserRelationshipService relationshipService;

    /**
     * Check if a user can access a question
     */
    @Transactional(readOnly = true)
    public boolean canAccessQuestion(QuizQuestion question, Long userId) {
        if (question == null || userId == null) {
            return false;
        }

        // Creator always has access
        if (question.getCreator() != null && question.getCreator().getId().equals(userId)) {
            return true;
        }

        QuestionVisibility visibility = question.getVisibility();

        switch (visibility) {
            case PUBLIC:
                return true;

            case PRIVATE:
                return false;

            case FRIENDS_FAMILY:
                // Check if user is connected to creator
                return relationshipService.areUsersConnected(question.getCreator().getId(), userId);

            case QUIZ_ONLY:
                // Check if user has access to the original quiz
                if (question.getOriginalQuiz() == null) {
                    return false;
                }
                // This would need to check challenge access
                // For now, allow if quiz is public or user is quiz creator
                return question.getOriginalQuiz().isPublic() ||
                        question.getOriginalQuiz().getCreator().getId().equals(userId);

            default:
                return false;
        }
    }

    /**
     * Verify access and throw exception if denied
     */
    public void verifyAccess(QuizQuestion question, Long userId, QuestionAccessType accessType) {
        if (!canAccessQuestion(question, userId)) {
            log.warn("Access denied for user {} to question {} (type: {})",
                    userId, question.getId(), accessType);
            throw new AccessDeniedException("You do not have permission to access this question");
        }

        // Additional checks for specific access types
        if (accessType == QuestionAccessType.EDIT || accessType == QuestionAccessType.DELETE) {
            if (!question.getCreator().getId().equals(userId)) {
                throw new AccessDeniedException("Only the creator can modify this question");
            }
        }
    }

    /**
     * Log access to a question
     */
    @Transactional
    public void logAccess(QuizQuestion question, User user, QuestionAccessType accessType) {
        QuestionAccessLog accessLog = QuestionAccessLog.builder()
                .question(question)
                .accessedByUser(user)
                .accessType(accessType)
                .build();

        accessLogRepository.save(accessLog);
        log.info("Logged {} access to question {} by user {}", accessType, question.getId(), user.getId());
    }

    /**
     * Get access logs for a question
     */
    @Transactional(readOnly = true)
    public List<QuestionAccessLog> getQuestionAccessLogs(Long questionId) {
        return accessLogRepository.findByQuestionIdOrderByAccessedAtDesc(questionId);
    }
}
