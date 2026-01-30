package com.my.challenger.repository;

import com.my.challenger.entity.quiz.TopicTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicTranslationRepository extends JpaRepository<TopicTranslation, Long> {
    
    Optional<TopicTranslation> findByTopicIdAndLanguageCode(Long topicId, String languageCode);
    
    List<TopicTranslation> findAllByTopicIdIn(List<Long> topicIds);
    
    List<TopicTranslation> findAllByLanguageCode(String languageCode);

    List<TopicTranslation> findAllByTopicIdInAndLanguageCode(List<Long> topicIds, String languageCode);
}
