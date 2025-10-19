package com.my.challenger.repository;

import com.my.challenger.entity.PaymentTransaction;
import com.my.challenger.entity.PaymentTransaction.TransactionStatus;
import com.my.challenger.entity.PaymentTransaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Find all transactions for a user
     */
    Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find all transactions for a challenge
     */
    Page<PaymentTransaction> findByChallengeIdOrderByCreatedAtDesc(Long challengeId, Pageable pageable);

    /**
     * Find transactions by user and challenge
     */
    List<PaymentTransaction> findByUserIdAndChallengeId(Long userId, Long challengeId);

    /**
     * Find transactions by status
     */
    List<PaymentTransaction> findByStatus(TransactionStatus status);

    /**
     * Find transactions by type
     */
    List<PaymentTransaction> findByTransactionType(TransactionType type);

    /**
     * Find pending transactions older than specified date
     */
    @Query("SELECT t FROM PaymentTransaction t WHERE " +
           "t.status = 'PENDING' AND t.createdAt < :cutoffDate")
    List<PaymentTransaction> findPendingTransactionsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Calculate total amount collected for a challenge
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE " +
           "t.challenge.id = :challengeId AND " +
           "t.transactionType = 'ENTRY_FEE' AND " +
           "t.status = 'COMPLETED'")
    BigDecimal getTotalCollectedForChallenge(@Param("challengeId") Long challengeId);

    /**
     * Calculate total prizes distributed for a challenge
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE " +
           "t.challenge.id = :challengeId AND " +
           "t.transactionType = 'PRIZE' AND " +
           "t.status = 'COMPLETED'")
    BigDecimal getTotalPrizesDistributed(@Param("challengeId") Long challengeId);

    /**
     * Get user's total spending
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE " +
           "t.user.id = :userId AND " +
           "t.transactionType IN ('ENTRY_FEE', 'WITHDRAWAL') AND " +
           "t.status = 'COMPLETED'")
    BigDecimal getUserTotalSpending(@Param("userId") Long userId);

    /**
     * Get user's total earnings
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE " +
           "t.user.id = :userId AND " +
           "t.transactionType IN ('PRIZE', 'DEPOSIT') AND " +
           "t.status = 'COMPLETED'")
    BigDecimal getUserTotalEarnings(@Param("userId") Long userId);

    /**
     * Count completed transactions for a user
     */
    long countByUserIdAndStatus(Long userId, TransactionStatus status);

    /**
     * Find transaction by reference
     */
    List<PaymentTransaction> findByTransactionReference(String reference);

    /**
     * Get transactions within date range
     */
    @Query("SELECT t FROM PaymentTransaction t WHERE " +
           "t.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY t.createdAt DESC")
    List<PaymentTransaction> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Get user's transaction history with filters
     */
    @Query("SELECT t FROM PaymentTransaction t WHERE " +
           "t.user.id = :userId AND " +
           "(:type IS NULL OR t.transactionType = :type) AND " +
           "(:status IS NULL OR t.status = :status) " +
           "ORDER BY t.createdAt DESC")
    Page<PaymentTransaction> findUserTransactionsWithFilters(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            Pageable pageable
    );
}