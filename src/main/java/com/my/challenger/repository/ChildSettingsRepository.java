package com.my.challenger.repository;

import com.my.challenger.entity.parental.ChildSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChildSettingsRepository extends JpaRepository<ChildSettings, Long> {
    Optional<ChildSettings> findByChildId(Long childId);
    Optional<ChildSettings> findByChildIdAndManagedByParentId(Long childId, Long parentId);
}
