package com.my.challenger.repository;

import com.my.challenger.entity.AudioSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AudioSubmissionRepository extends JpaRepository<AudioSubmission, Long> {

    List<AudioSubmission> findByQuestionIdOrderByCreatedAtDesc(Long questionId);

    List<AudioSubmission> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AudioSubmission> findByQuestionIdAndUserId(Long questionId, Long userId);

    List<AudioSubmission> findByQuestionIdAndUserIdOrderByCreatedAtDesc(Long questionId, Long userId);

    Optional<AudioSubmission> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT s FROM AudioSubmission s WHERE s.question.id = :questionId " +
           "AND s.userId = :userId AND s.processingStatus = 'COMPLETED' " +
           "ORDER BY s.overallScore DESC")
    List<AudioSubmission> findBestSubmissions(
            @Param("questionId") Long questionId,
            @Param("userId") Long userId);

    @Query("SELECT s FROM AudioSubmission s WHERE s.question.id = :questionId " +
           "AND s.userId = :userId AND s.processingStatus = 'COMPLETED' " +
           "ORDER BY s.overallScore DESC LIMIT 1")
    Optional<AudioSubmission> findBestSubmission(
            @Param("questionId") Long questionId,
            @Param("userId") Long userId);

    @Query("SELECT s FROM AudioSubmission s WHERE s.processingStatus = 'PENDING' " +
           "ORDER BY s.createdAt ASC")
    List<AudioSubmission> findPendingSubmissions();

    @Query("SELECT AVG(s.overallScore) FROM AudioSubmission s " +
           "WHERE s.question.id = :questionId AND s.processingStatus = 'COMPLETED'")
    Optional<Double> getAverageScoreForQuestion(@Param("questionId") Long questionId);
}
