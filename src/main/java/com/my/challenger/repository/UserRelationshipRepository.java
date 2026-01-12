// UserRelationshipRepository.java
package com.my.challenger.repository;

import com.my.challenger.entity.User;
import com.my.challenger.entity.UserRelationship;
import com.my.challenger.entity.enums.RelationshipStatus;
import com.my.challenger.entity.enums.RelationshipType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRelationshipRepository extends JpaRepository<UserRelationship, Long> {
    
    /**
     * Find relationship between two users (bidirectional)
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "(ur.user.id = :userId AND ur.relatedUser.id = :relatedUserId) OR " +
           "(ur.user.id = :relatedUserId AND ur.relatedUser.id = :userId)")
    Optional<UserRelationship> findBetweenUsers(@Param("userId") Long userId, 
                                                 @Param("relatedUserId") Long relatedUserId);
    
    /**
     * Find all relationships for a user
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "ur.user.id = :userId OR ur.relatedUser.id = :userId")
    List<UserRelationship> findAllForUser(@Param("userId") Long userId);

    /**
     * Find relationships for a user with filters and pagination
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "(ur.user.id = :userId OR ur.relatedUser.id = :userId) " +
           "AND (:relatedUserId IS NULL OR ur.user.id = :relatedUserId OR ur.relatedUser.id = :relatedUserId) " +
           "AND (:type IS NULL OR ur.relationshipType = :type) " +
           "AND (:status IS NULL OR ur.status = :status)")
    Page<UserRelationship> findFiltered(@Param("userId") Long userId, 
                                       @Param("relatedUserId") Long relatedUserId,
                                       @Param("type") RelationshipType type, 
                                       @Param("status") RelationshipStatus status, 
                                       Pageable pageable);
    
    /**
     * Find favorite relationships for a user
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "(ur.user.id = :userId OR ur.relatedUser.id = :userId) " +
           "AND ur.isFavorite = true")
    List<UserRelationship> findFavorites(@Param("userId") Long userId);
    
    /**
     * Count mutual connections between two users
     */
    @Query("SELECT COUNT(u.id) FROM User u WHERE u.id IN (" +
           "  SELECT CASE WHEN ur1.user.id = :userId1 THEN ur1.relatedUser.id ELSE ur1.user.id END " +
           "  FROM UserRelationship ur1 " +
           "  WHERE (ur1.user.id = :userId1 OR ur1.relatedUser.id = :userId1) AND ur1.status = 'ACCEPTED' " +
           "  AND ur1.relationshipType != 'BLOCKED'" +
           ") AND u.id IN (" +
           "  SELECT CASE WHEN ur2.user.id = :userId2 THEN ur2.relatedUser.id ELSE ur2.user.id END " +
           "  FROM UserRelationship ur2 " +
           "  WHERE (ur2.user.id = :userId2 OR ur2.relatedUser.id = :userId2) AND ur2.status = 'ACCEPTED' " +
           "  AND ur2.relationshipType != 'BLOCKED'" +
           ")")
    long countMutualConnections(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Find mutual connections between two users
     */
    @Query("SELECT u FROM User u WHERE u.id IN (" +
           "  SELECT CASE WHEN ur1.user.id = :userId1 THEN ur1.relatedUser.id ELSE ur1.user.id END " +
           "  FROM UserRelationship ur1 " +
           "  WHERE (ur1.user.id = :userId1 OR ur1.relatedUser.id = :userId1) AND ur1.status = 'ACCEPTED' " +
           "  AND ur1.relationshipType != 'BLOCKED'" +
           ") AND u.id IN (" +
           "  SELECT CASE WHEN ur2.user.id = :userId2 THEN ur2.relatedUser.id ELSE ur2.user.id END " +
           "  FROM UserRelationship ur2 " +
           "  WHERE (ur2.user.id = :userId2 OR ur2.relatedUser.id = :userId2) AND ur2.status = 'ACCEPTED' " +
           "  AND ur2.relationshipType != 'BLOCKED'" +
           ")")
    List<User> findMutualConnections(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Find suggested connections (friends of friends not yet connected)
     */
    @Query("SELECT u FROM User u WHERE u.id != :userId " +
           "AND u.id NOT IN (SELECT CASE WHEN ur.user.id = :userId THEN ur.relatedUser.id ELSE ur.user.id END FROM UserRelationship ur WHERE ur.user.id = :userId OR ur.relatedUser.id = :userId) " +
           "AND u.id IN (" +
           "  SELECT CASE WHEN ur2.user.id = f.friendId THEN ur2.relatedUser.id ELSE ur2.user.id END " +
           "  FROM UserRelationship ur2, " +
           "  (SELECT CASE WHEN ur1.user.id = :userId THEN ur1.relatedUser.id ELSE ur1.user.id END as friendId " +
           "   FROM UserRelationship ur1 WHERE (ur1.user.id = :userId OR ur1.relatedUser.id = :userId) AND ur1.status = 'ACCEPTED') f " +
           "  WHERE (ur2.user.id = f.friendId OR ur2.relatedUser.id = f.friendId) AND ur2.status = 'ACCEPTED' " +
           ")")
    List<User> findSuggestedConnections(@Param("userId") Long userId);

    /**
     * Find accepted friends and family for a user
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "(ur.user.id = :userId OR ur.relatedUser.id = :userId) " +
           "AND ur.status = :status " +
           "AND ur.relationshipType IN :types")
    List<UserRelationship> findAcceptedRelationships(@Param("userId") Long userId,
                                                      @Param("status") RelationshipStatus status,
                                                      @Param("types") List<RelationshipType> types);
    
    /**
     * Check if two users have an accepted relationship
     */
    @Query("SELECT CASE WHEN COUNT(ur) > 0 THEN true ELSE false END " +
           "FROM UserRelationship ur WHERE " +
           "((ur.user.id = :userId AND ur.relatedUser.id = :relatedUserId) OR " +
           " (ur.user.id = :relatedUserId AND ur.relatedUser.id = :userId)) " +
           "AND ur.status = 'ACCEPTED' " +
           "AND ur.relationshipType IN ('FRIEND', 'FAMILY')")
    boolean areUsersConnected(@Param("userId") Long userId, 
                              @Param("relatedUserId") Long relatedUserId);
    
    /**
     * Get all friend and family IDs for a user
     */
    @Query("SELECT CASE WHEN ur.user.id = :userId THEN ur.relatedUser.id ELSE ur.user.id END " +
           "FROM UserRelationship ur WHERE " +
           "(ur.user.id = :userId OR ur.relatedUser.id = :userId) " +
           "AND ur.status = 'ACCEPTED' " +
           "AND ur.relationshipType IN ('FRIEND', 'FAMILY')")
    List<Long> findConnectedUserIds(@Param("userId") Long userId);
    
    /**
     * Find pending relationship requests for a user
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "ur.relatedUser.id = :userId " +
           "AND ur.status = 'PENDING'")
    List<UserRelationship> findPendingRequestsForUser(@Param("userId") Long userId);
}