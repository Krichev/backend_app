package com.my.challenger.service.impl;

import com.my.challenger.entity.PaymentTransaction;
import com.my.challenger.entity.PaymentTransaction.TransactionStatus;
import com.my.challenger.entity.PaymentTransaction.TransactionType;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.CurrencyType;
import com.my.challenger.repository.PaymentTransactionRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Enhanced Payment Service with full transaction recording
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EnhancedPaymentService {

    private final UserRepository userRepository;
    private final PaymentTransactionRepository transactionRepository;
    // TODO: Inject Stripe/PayPal service when ready

    /**
     * Process entry fee payment for a challenge
     */
    @Transactional
    public PaymentTransaction processEntryFee(User user, Challenge challenge, 
                                             BigDecimal amount, CurrencyType currency) {
        log.info("Processing entry fee for user {} on challenge {}", user.getId(), challenge.getId());

        // Create transaction record
        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(user)
                .challenge(challenge)
                .transactionType(TransactionType.ENTRY_FEE)
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.PENDING)
                .paymentMethod(currency == CurrencyType.POINTS ? "POINTS" : "CASH")
                .build();

        try {
            if (currency == CurrencyType.POINTS) {
                // Process points payment
                deductPoints(user, amount.longValue());
                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.markCompleted();
            } else {
                // Process cash payment
                // TODO: Integrate with payment gateway
                processCashPayment(user, amount, currency);
                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.markCompleted();
            }

            // Add to challenge prize pool
            challenge.addEntryFee(amount);

            transaction = transactionRepository.save(transaction);
            log.info("Entry fee processed successfully. Transaction ID: {}", transaction.getId());
            return transaction;

        } catch (Exception e) {
            transaction.markFailed(e.getMessage());
            transactionRepository.save(transaction);
            log.error("Failed to process entry fee", e);
            throw new RuntimeException("Payment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Distribute prize to winner
     */
    @Transactional
    public PaymentTransaction distributePrize(User winner, Challenge challenge, 
                                             BigDecimal amount, CurrencyType currency) {
        log.info("Distributing prize to user {} for challenge {}", winner.getId(), challenge.getId());

        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(winner)
                .challenge(challenge)
                .transactionType(TransactionType.PRIZE)
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.PENDING)
                .paymentMethod(currency == CurrencyType.POINTS ? "POINTS" : "CASH")
                .notes("Prize for winning challenge: " + challenge.getTitle())
                .build();

        try {
            if (currency == CurrencyType.POINTS) {
                addPoints(winner, amount.longValue());
                transaction.markCompleted();
            } else {
                // TODO: Transfer cash to user's account via payment gateway
                transferCashPrize(winner, amount, currency);
                transaction.markCompleted();
            }

            transaction = transactionRepository.save(transaction);
            log.info("Prize distributed successfully. Transaction ID: {}", transaction.getId());
            return transaction;

        } catch (Exception e) {
            transaction.markFailed(e.getMessage());
            transactionRepository.save(transaction);
            log.error("Failed to distribute prize", e);
            throw new RuntimeException("Prize distribution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Refund entry fee
     */
    @Transactional
    public PaymentTransaction refundEntryFee(User user, Challenge challenge, 
                                            BigDecimal amount, CurrencyType currency, String reason) {
        log.info("Refunding entry fee to user {} for challenge {}", user.getId(), challenge.getId());

        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(user)
                .challenge(challenge)
                .transactionType(TransactionType.REFUND)
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.PENDING)
                .paymentMethod(currency == CurrencyType.POINTS ? "POINTS" : "CASH")
                .notes("Refund reason: " + reason)
                .build();

        try {
            if (currency == CurrencyType.POINTS) {
                addPoints(user, amount.longValue());
                transaction.markCompleted();
            } else {
                // TODO: Process cash refund via payment gateway
                processCashRefund(user, amount, currency);
                transaction.markCompleted();
            }

            transaction = transactionRepository.save(transaction);
            log.info("Refund processed successfully. Transaction ID: {}", transaction.getId());
            return transaction;

        } catch (Exception e) {
            transaction.markFailed(e.getMessage());
            transactionRepository.save(transaction);
            log.error("Failed to process refund", e);
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deduct points from user's account
     */
    @Transactional
    public void deductPoints(User user, Long points) {
        Long currentPoints = user.getPoints();
        
        if (currentPoints == null || currentPoints < points) {
            throw new IllegalStateException(
                String.format("Insufficient points. Required: %d, Available: %d", 
                             points, currentPoints != null ? currentPoints : 0)
            );
        }

        user.deductPoints(points);
        userRepository.save(user);
        log.info("Deducted {} points from user {}", points, user.getId());
    }

    /**
     * Add points to user's account
     */
    @Transactional
    public void addPoints(User user, Long points) {
        user.addPoints(points);
        userRepository.save(user);
        log.info("Added {} points to user {}", points, user.getId());
    }

    /**
     * Process cash payment (integrate with payment gateway)
     */
    private void processCashPayment(User user, BigDecimal amount, CurrencyType currency) {
        // TODO: Integrate with Stripe, PayPal, or other payment gateway
        log.info("Processing cash payment of {} {} for user {}", amount, currency, user.getId());
        
        // Example Stripe integration:
        // PaymentIntent intent = stripeService.createPaymentIntent(amount, currency);
        // String clientSecret = intent.getClientSecret();
        // return clientSecret; // Send to frontend to complete payment
        
        // For now, just log
        log.info("Cash payment would be processed here");
    }

    /**
     * Transfer cash prize to winner
     */
    private void transferCashPrize(User winner, BigDecimal amount, CurrencyType currency) {
        // TODO: Integrate with payout API (Stripe Connect, PayPal Payouts, etc.)
        log.info("Transferring cash prize of {} {} to user {}", amount, currency, winner.getId());
        
        // Example:
        // Transfer transfer = stripeService.createTransfer(winner.getStripeAccountId(), amount);
        
        log.info("Cash prize would be transferred here");
    }

    /**
     * Process cash refund
     */
    private void processCashRefund(User user, BigDecimal amount, CurrencyType currency) {
        // TODO: Process refund via payment gateway
        log.info("Processing cash refund of {} {} to user {}", amount, currency, user.getId());
        
        // Example:
        // Refund refund = stripeService.createRefund(originalChargeId, amount);
        
        log.info("Cash refund would be processed here");
    }

    /**
     * Check if user has sufficient balance
     */
    public boolean hasSufficientBalance(User user, BigDecimal amount, CurrencyType currency) {
        if (currency == CurrencyType.POINTS) {
            Long userPoints = user.getPoints() != null ? user.getPoints() : 0L;
            return userPoints >= amount.longValue();
        } else {
            // For cash, assume they'll provide payment method at checkout
            return true;
        }
    }

    /**
     * Get user's transaction history
     */
    public PaymentTransaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    /**
     * Cancel pending transaction
     */
    @Transactional
    public void cancelTransaction(Long transactionId, String reason) {
        PaymentTransaction transaction = getTransactionById(transactionId);
        
        if (!transaction.isPending()) {
            throw new IllegalStateException("Can only cancel pending transactions");
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setNotes(transaction.getNotes() + "\nCancelled: " + reason);
        transactionRepository.save(transaction);
        
        log.info("Transaction {} cancelled: {}", transactionId, reason);
    }

    /**
     * Get total collected for a challenge
     */
    public BigDecimal getTotalCollectedForChallenge(Long challengeId) {
        return transactionRepository.getTotalCollectedForChallenge(challengeId);
    }

    /**
     * Get user's spending and earnings
     */
    public UserFinancialSummary getUserFinancialSummary(Long userId) {
        BigDecimal totalSpent = transactionRepository.getUserTotalSpending(userId);
        BigDecimal totalEarned = transactionRepository.getUserTotalEarnings(userId);
        
        return new UserFinancialSummary(totalSpent, totalEarned, totalEarned.subtract(totalSpent));
    }

    // Inner class for financial summary
    public static class UserFinancialSummary {
        private final BigDecimal totalSpent;
        private final BigDecimal totalEarned;
        private final BigDecimal netBalance;

        public UserFinancialSummary(BigDecimal totalSpent, BigDecimal totalEarned, BigDecimal netBalance) {
            this.totalSpent = totalSpent;
            this.totalEarned = totalEarned;
            this.netBalance = netBalance;
        }

        public BigDecimal getTotalSpent() { return totalSpent; }
        public BigDecimal getTotalEarned() { return totalEarned; }
        public BigDecimal getNetBalance() { return netBalance; }
    }
}