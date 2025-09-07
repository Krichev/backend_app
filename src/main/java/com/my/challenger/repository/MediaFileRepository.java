package com.my.challenger.repository;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByEntityIdAndMediaType(Long entityId, MediaType mediaType);

    List<MediaFile> findByUploadedBy(Long uploadedBy);

    List<MediaFile> findByProcessingStatus(ProcessingStatus status);

    Optional<MediaFile> findByFilename(String filename);

    Optional<MediaFile> findByS3Key(String s3Key);

    void deleteByEntityIdAndMediaType(Long entityId, MediaType mediaType);
}