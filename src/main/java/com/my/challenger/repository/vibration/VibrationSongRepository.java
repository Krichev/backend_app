package com.my.challenger.repository.vibration;

import com.my.challenger.entity.enums.SongStatus;
import com.my.challenger.entity.enums.VibrationDifficulty;
import com.my.challenger.entity.vibration.VibrationSong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VibrationSongRepository extends JpaRepository<VibrationSong, Long> {

    Optional<VibrationSong> findByExternalId(UUID externalId);

    Page<VibrationSong> findByStatusAndDifficultyAndCategoryContaining(
            SongStatus status, VibrationDifficulty difficulty, String category, Pageable pageable);

    @Query(value = """
        SELECT * FROM vibration_songs 
        WHERE status = 'APPROVED' 
        AND (:difficulty IS NULL OR difficulty = :difficulty)
        AND (:category IS NULL OR category = :category)
        AND (:excludeIds IS NULL OR id NOT IN :excludeIds)
        ORDER BY RANDOM() 
        LIMIT :count
        """, nativeQuery = true)
    List<VibrationSong> findRandomSongs(
            @Param("count") int count,
            @Param("difficulty") String difficulty,
            @Param("category") String category,
            @Param("excludeIds") List<Long> excludeIds);

    @Query("SELECT DISTINCT s.category FROM VibrationSong s WHERE s.category IS NOT NULL AND s.status = 'APPROVED'")
    List<String> findAllCategories();

    @Query("SELECT s.category, COUNT(s) FROM VibrationSong s WHERE s.status = 'APPROVED' GROUP BY s.category")
    List<Object[]> getCategoryCounts();

    Page<VibrationSong> findByCreatorId(String creatorId, Pageable pageable);

    Page<VibrationSong> findByStatus(SongStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE VibrationSong s SET s.playCount = s.playCount + 1, s.totalAttempts = s.totalAttempts + 1 WHERE s.id = :id")
    void incrementPlayCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE VibrationSong s SET s.correctGuesses = s.correctGuesses + 1 WHERE s.id = :id")
    void incrementCorrectGuesses(@Param("id") Long id);
}
