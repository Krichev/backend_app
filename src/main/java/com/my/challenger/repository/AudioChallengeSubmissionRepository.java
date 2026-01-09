package com.my.challenger.repository;

import com.my.challenger.entity.AudioChallengeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AudioChallengeSubmissionRepository extends JpaRepository<AudioChallengeSubmission, Long> {

    List<AudioChallengeSubmission> findByQuestionIdOrderByCreatedAtDesc(Long questionId);

    List<AudioChallengeSubmission> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AudioChallengeSubmission> findByQuestionIdAndUserId(Long questionId, Long userId);

    @Query("SELECT s FROM AudioChallengeSubmission s WHERE s.question.id = :questionId " +
           "AND s.userId = :userId ORDER BY s.overallScore DESC")
    List<AudioChallengeSubmission> findBestSubmissions(
            @Param("questionId") Long questionId,
            @Param("userId") Long userId);

    @Query("SELECT s FROM AudioChallengeSubmission s WHERE s.processingStatus = 'PENDING' " +
           "ORDER BY s.createdAt ASC")
    List<AudioChallengeSubmission> findPendingSubmissions();

    @Query("SELECT AVG(s.overallScore) FROM AudioChallengeSubmission s " +
           "WHERE s.question.id = :questionId AND s.processingStatus = 'COMPLETED'")
    Optional<Double> getAverageScoreForQuestion(@Param("questionId") Long questionId);
}
