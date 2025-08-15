package com.my.challenger.repository;

import com.my.challenger.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Search users by username containing the search term (case insensitive)
     */
    List<User> findByUsernameContainingIgnoreCase(String searchTerm);

    /**
     * Find users by email containing the search term (case insensitive)
     */
    List<User> findByEmailContainingIgnoreCase(String searchTerm);

    /**
     * Get users with pagination and sorting
     */
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findAllOrderByCreatedAtDesc();

    /**
     * Find active users (you can add status field to User entity)
     */
    @Query("SELECT u FROM User u WHERE u.createdAt IS NOT NULL ORDER BY u.username ASC")
    List<User> findActiveUsers();

    /**
     * Count total users
     */
    @Query("SELECT COUNT(u) FROM User u")
    long countTotalUsers();

    /**
     * Find users by creation date range
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :startDate AND u.createdAt <= :endDate")
    List<User> findByCreatedAtBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                      @Param("endDate") java.time.LocalDateTime endDate);

}
