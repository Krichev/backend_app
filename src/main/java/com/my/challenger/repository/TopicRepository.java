package com.my.challenger.repository;

import com.my.challenger.entity.quiz.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    /**
     * Find topic by name (case-insensitive)
     */
    @Query("SELECT t FROM Topic t WHERE LOWER(t.name) = LOWER(:name)")
    Optional<Topic> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Find all active topics
     */
    List<Topic> findByIsActiveTrue();

    /**
     * Find topics by category
     */
    List<Topic> findByCategoryAndIsActiveTrue(String category);

    /**
     * Find topics with pagination
     */
    Page<Topic> findByIsActiveTrue(Pageable pageable);

    /**
     * Search topics by name containing (case-insensitive)
     */
    @Query("SELECT t FROM Topic t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND t.isActive = true")
    List<Topic> searchByNameContaining(@Param("searchTerm") String searchTerm);

    /**
     * Get all categories
     */
    @Query("SELECT DISTINCT t.category FROM Topic t WHERE t.category IS NOT NULL AND t.isActive = true ORDER BY t.category")
    List<String> findAllCategories();

    /**
     * Find topics by creator
     */
    List<Topic> findByCreatorIdAndIsActiveTrue(Long creatorId);

    /**
     * Get topics with most questions
     */
    @Query("SELECT t FROM Topic t WHERE t.isActive = true ORDER BY t.questionCount DESC")
    Page<Topic> findTopTopicsByQuestionCount(Pageable pageable);

    /**
     * Check if topic name exists
     */
    boolean existsByNameIgnoreCase(String name);
}