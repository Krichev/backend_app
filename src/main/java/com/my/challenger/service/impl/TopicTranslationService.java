package com.my.challenger.service.impl;

import com.my.challenger.entity.quiz.TopicTranslation;
import com.my.challenger.repository.TopicTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopicTranslationService {

    private final TopicTranslationRepository topicTranslationRepository;

    @Transactional(readOnly = true)
    public Optional<TopicTranslation> getTranslation(Long topicId, String languageCode) {
        if (topicId == null || languageCode == null || "en".equalsIgnoreCase(languageCode)) {
            return Optional.empty();
        }
        return topicTranslationRepository.findByTopicIdAndLanguageCode(topicId, languageCode);
    }

    @Transactional(readOnly = true)
    public Map<Long, TopicTranslation> getTranslationsForTopics(List<Long> topicIds, String languageCode) {
        if (topicIds == null || topicIds.isEmpty() || languageCode == null || "en".equalsIgnoreCase(languageCode)) {
            return Collections.emptyMap();
        }
        
        List<TopicTranslation> translations = topicTranslationRepository.findAllByTopicIdInAndLanguageCode(topicIds, languageCode);
        
        return translations.stream()
                .collect(Collectors.toMap(
                    t -> t.getTopic().getId(),
                    Function.identity(),
                    (existing, replacement) -> existing
                ));
    }
}
