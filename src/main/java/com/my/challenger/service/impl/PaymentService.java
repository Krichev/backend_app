package com.my.challenger.service.impl;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.CurrencyType;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Payment Service for handling points and cash transactions
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    // Inject payment gateway service for cash payments

    /**
     * Deduct points from user's account
     */
    @Transactional
    public void deductPoints(User user, Long points) {
        // Assuming User entity has a 'points' field
        Long currentPoints = user.getPoints();
        
        if (currentPoints == null || currentPoints < points) {
            throw new IllegalStateException("Insufficient points. Required: " + points + 
                                          ", Available: " + (currentPoints != null ? currentPoints : 0));
        }

        user.setPoints(currentPoints - points);
        userRepository.save(user);
        log.info("Deducted {} points from user {}", points, user.getId());
    }

    /**
     * Add points to user's account
     */
    @Transactional
    public void addPoints(User user, Long points) {
        Long currentPoints = user.getPoints() != null ? user.getPoints() : 0L;
        user.setPoints(currentPoints + points);
        userRepository.save(user);
        log.info("Added {} points to user {}", points, user.getId());
    }

    /**
     * Process cash payment (integrate with payment gateway)
     */
    @Transactional
    public void processCashPayment(User user, BigDecimal amount, CurrencyType currency) {
        // TODO: Integrate with Stripe, PayPal, or other payment gateway
        log.info("Processing cash payment of {} {} for user {}", amount, currency, user.getId());
        
        // Example integration:
        // 1. Create payment intent with your payment provider
        // 2. Verify payment status
        // 3. Record transaction
        
        // For now, just log
        log.info("Cash payment processed successfully");
    }

    /**
     * Distribute prize to winner(s)
     */
    @Transactional
    public void distributePrize(User user, BigDecimal amount, CurrencyType currency) {
        if (currency == CurrencyType.POINTS) {
            addPoints(user, amount.longValue());
        } else {
            // Transfer cash to user's account
            // TODO: Integrate with payout API
            log.info("Distributing cash prize of {} {} to user {}", amount, currency, user.getId());
        }
    }

    /**
     * Refund entry fee
     */
    @Transactional
    public void refundEntryFee(User user, BigDecimal amount, CurrencyType currency) {
        if (currency == CurrencyType.POINTS) {
            addPoints(user, amount.longValue());
        } else {
            // Process cash refund
            log.info("Refunding {} {} to user {}", amount, currency, user.getId());
        }
    }

    /**
     * Check if user has sufficient balance
     */
    public boolean hasSufficientBalance(User user, BigDecimal amount, CurrencyType currency) {
        if (currency == CurrencyType.POINTS) {
            Long userPoints = user.getPoints() != null ? user.getPoints() : 0L;
            return userPoints >= amount.longValue();
        } else {
            // Check with payment provider if user has payment method on file
            return true; // Simplified for example
        }
    }
}