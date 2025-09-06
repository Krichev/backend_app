package com.my.challenger.repository;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    Optional<MediaFile> findByEntityIdAndMediaType(Long entityId, MediaType mediaType);

    List<MediaFile> findByMediaTypeAndUploadedBy(MediaType mediaType, Long uploadedBy);

    List<MediaFile> findByMediaCategoryAndUploadedBy(MediaCategory mediaCategory, Long uploadedBy);

    @Query("SELECT m FROM MediaFile m WHERE m.mediaType = :type AND m.entityId IN :entityIds")
    List<MediaFile> findByTypeAndEntityIds(@Param("type") MediaType mediaType,
                                           @Param("entityIds") List<Long> entityIds);

    List<MediaFile> findByProcessingStatus(ProcessingStatus status);

    void deleteByEntityIdAndMediaType(Long entityId, MediaType mediaType);

    @Query("SELECT COUNT(m) FROM MediaFile m WHERE m.uploadedBy = :userId")
    Long countMediaByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(m.fileSize) FROM MediaFile m WHERE m.uploadedBy = :userId")
    Long getTotalStorageByUser(@Param("userId") Long userId);
}
