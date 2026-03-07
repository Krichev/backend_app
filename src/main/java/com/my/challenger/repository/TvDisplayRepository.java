package com.my.challenger.repository;

import com.my.challenger.entity.TvDisplay;
import com.my.challenger.entity.enums.TvDisplayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TvDisplayRepository extends JpaRepository<TvDisplay, Long> {
    Optional<TvDisplay> findByPairingCodeAndStatus(String pairingCode, TvDisplayStatus status);
    Optional<TvDisplay> findByIdAndToken(Long id, String token);
    List<TvDisplay> findByStatusAndExpiresAtBefore(TvDisplayStatus status, LocalDateTime now);
    void deleteByStatusAndExpiresAtBefore(TvDisplayStatus status, LocalDateTime now);
}
