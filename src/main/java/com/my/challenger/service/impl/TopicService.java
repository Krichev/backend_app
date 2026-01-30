package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.CreateTopicRequest;
import com.my.challenger.dto.quiz.TopicResponse;
import com.my.challenger.dto.quiz.TopicTreeResponse;
import com.my.challenger.dto.quiz.UpdateTopicRequest;
import com.my.challenger.dto.quiz.ValidateTopicRequest;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.ValidationStatus;
import com.my.challenger.entity.quiz.Topic;
import com.my.challenger.entity.quiz.TopicTranslation;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.TopicRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopicService {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final TopicTranslationService topicTranslationService;

    /**
     * Create a new topic
     */
    @Transactional
    public TopicResponse createTopic(CreateTopicRequest request) {
        log.info("Creating topic: {}", request.getName());

        // Check if topic already exists
        if (topicRepository.existsByNameIgnoreCase(request.getName())) {
            throw new RuntimeException("Topic with name '" + request.getName() + "' already exists");
        }

        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create topic
        Topic topic = Topic.builder()
                .name(request.getName().trim())
                .category(request.getCategory())
                .description(request.getDescription())
                .creator(creator)
                .isActive(true)
                .questionCount(0)
                .build();

        Topic savedTopic = topicRepository.save(topic);
        log.info("Topic created successfully with ID: {}", savedTopic.getId());

        return mapToResponse(savedTopic, null);
    }

    /**
     * Get topic by ID
     */
    @Transactional(readOnly = true)
    public TopicResponse getTopicById(Long id, String languageCode) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));
        return mapToResponse(topic, languageCode);
    }

    /**
     * Get topic by name
     */
    @Transactional(readOnly = true)
    public TopicResponse getTopicByName(String name, String languageCode) {
        Topic topic = topicRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with name: " + name));
        return mapToResponse(topic, languageCode);
    }

    /**
     * Get or create topic by name
     */
    @Transactional
    public Topic getOrCreateTopic(String topicName) {
        return topicRepository.findByNameIgnoreCase(topicName)
                .orElseGet(() -> {
                    log.info("Creating new topic: {}", topicName);
                    Topic newTopic = Topic.builder()
                            .name(topicName.trim())
                            .isActive(true)
                            .questionCount(0)
                            .build();
                    return topicRepository.save(newTopic);
                });
    }

    /**
     * Get all active topics
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> getAllTopics(String languageCode) {
        return mapTopicsToResponses(topicRepository.findByIsActiveTrue(), languageCode);
    }

    /**
     * Get topics with pagination
     */
    @Transactional(readOnly = true)
    public Page<TopicResponse> getTopics(Pageable pageable, String languageCode) {
        Page<Topic> topicsPage = topicRepository.findByIsActiveTrue(pageable);
        // We need to fetch translations for the content of the page
        List<Topic> topics = topicsPage.getContent();
        Map<Long, TopicTranslation> translations = getTranslationsMap(topics, languageCode);
        
        return topicsPage.map(topic -> mapToResponseWithTranslation(topic, translations.get(topic.getId())));
    }

    /**
     * Get topics by category
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByCategory(String category, String languageCode) {
        return mapTopicsToResponses(topicRepository.findByCategoryAndIsActiveTrue(category), languageCode);
    }

    /**
     * Search topics by name
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> searchTopics(String searchTerm, String languageCode) {
        return mapTopicsToResponses(topicRepository.searchByNameContaining(searchTerm), languageCode);
    }

    /**
     * Get all categories
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return topicRepository.findAllCategories();
    }

    /**
     * Get popular topics (most questions)
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> getPopularTopics(int limit, String languageCode) {
        Pageable pageable = Pageable.ofSize(limit);
        return mapTopicsToResponses(topicRepository.findTopTopicsByQuestionCount(pageable), languageCode);
    }

    /**
     * Update topic
     */
    @Transactional
    public TopicResponse updateTopic(Long id, UpdateTopicRequest request) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));

        // Check if new name already exists (if name is being changed)
        if (request.getName() != null && !request.getName().equalsIgnoreCase(topic.getName())) {
            if (topicRepository.existsByNameIgnoreCase(request.getName())) {
                throw new RuntimeException("Topic with name '" + request.getName() + "' already exists");
            }
            topic.setName(request.getName().trim());
        }

        if (request.getCategory() != null) {
            topic.setCategory(request.getCategory());
        }

        if (request.getDescription() != null) {
            topic.setDescription(request.getDescription());
        }

        if (request.getIsActive() != null) {
            topic.setIsActive(request.getIsActive());
        }

        Topic updatedTopic = topicRepository.save(topic);
        log.info("Topic updated successfully: {}", updatedTopic.getId());

        return mapToResponse(updatedTopic, null);
    }

    /**
     * Delete topic (soft delete)
     */
    @Transactional
    public void deleteTopic(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));

        topic.setIsActive(false);
        topicRepository.save(topic);
        log.info("Topic soft deleted: {}", id);
    }

    /**
     * Increment question count for topic
     */
    @Transactional
    public void incrementQuestionCount(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        topic.incrementQuestionCount();
        topicRepository.save(topic);
    }

    /**
     * Decrement question count for topic
     */
    @Transactional
    public void decrementQuestionCount(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        topic.decrementQuestionCount();
        topicRepository.save(topic);
    }

    // Helper method to map entity to response
    private TopicResponse mapToResponse(Topic topic, String languageCode) {
        TopicTranslation translation = null;
        if (languageCode != null && !"en".equalsIgnoreCase(languageCode)) {
            translation = topicTranslationService.getTranslation(topic.getId(), languageCode).orElse(null);
        }
        return mapToResponseWithTranslation(topic, translation);
    }

    private TopicResponse mapToResponseWithTranslation(Topic topic, TopicTranslation translation) {
        String name = topic.getName();
        String description = topic.getDescription();
        String originalName = null;
        String originalDescription = null;

        if (translation != null) {
            name = translation.getName();
            description = translation.getDescription();
            originalName = topic.getName();
            originalDescription = topic.getDescription();
        }

        return TopicResponse.builder()
                .id(topic.getId())
                .name(name)
                .category(topic.getCategory())
                .description(description)
                .originalName(originalName)
                .originalDescription(originalDescription)
                .questionCount(topic.getQuestionCount())
                .isActive(topic.getIsActive())
                .creatorId(topic.getCreator() != null ? topic.getCreator().getId() : null)
                .parentId(topic.getParent() != null ? topic.getParent().getId() : null)
                .parentName(topic.getParent() != null ? topic.getParent().getName() : null) // Note: parentName is not translated here to avoid extra queries, or we could fetch parent translation
                .path(topic.getPath())
                .depth(topic.getDepth())
                .isSystemTopic(topic.getIsSystemTopic())
                .validationStatus(topic.getValidationStatus())
                .slug(topic.getSlug())
                .childCount(topic.getChildCount())
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .validatedAt(topic.getValidatedAt())
                .build();
    }

    /**
     * Helper method to map entity to response with accurate childCount from database
     * Used for tree navigation where we need to know if a node is expandable
     */
    private TopicResponse mapToResponseWithChildCount(Topic topic, TopicTranslation translation) {
        TopicResponse response = mapToResponseWithTranslation(topic, translation);
        int childCount = topicRepository.countByParentIdAndIsActiveTrue(topic.getId());
        response.setChildCount(childCount);
        return response;
    }

    // Bulk mapping helpers
    private Map<Long, TopicTranslation> getTranslationsMap(List<Topic> topics, String languageCode) {
        if (topics == null || topics.isEmpty() || languageCode == null || "en".equalsIgnoreCase(languageCode)) {
            return Collections.emptyMap();
        }
        List<Long> topicIds = topics.stream().map(Topic::getId).collect(Collectors.toList());
        return topicTranslationService.getTranslationsForTopics(topicIds, languageCode);
    }

    private List<TopicResponse> mapTopicsToResponses(List<Topic> topics, String languageCode) {
        Map<Long, TopicTranslation> translations = getTranslationsMap(topics, languageCode);
        return topics.stream()
                .map(topic -> mapToResponseWithTranslation(topic, translations.get(topic.getId())))
                .collect(Collectors.toList());
    }

    public Topic findOrCreateTopic(String topic, User creator) {
        return null;
    }

    /**
     * Get direct children of a topic
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicChildren(Long parentId, String languageCode) {
        log.info("Fetching children for topic: {}", parentId);

        // Verify parent topic exists
        if (!topicRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Topic not found with id: " + parentId);
        }

        List<Topic> children = topicRepository.findByParentIdAndIsActiveTrue(parentId).stream()
                .sorted((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()))
                .collect(Collectors.toList());
        
        Map<Long, TopicTranslation> translations = getTranslationsMap(children, languageCode);

        return children.stream()
                .map(topic -> mapToResponseWithChildCount(topic, translations.get(topic.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get root topics (no parent)
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> getRootTopics(String languageCode) {
        log.info("Fetching root topics");

        List<Topic> roots = topicRepository.findByParentIsNullAndIsActiveTrue().stream()
                .sorted((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()))
                .collect(Collectors.toList());
        
        Map<Long, TopicTranslation> translations = getTranslationsMap(roots, languageCode);

        return roots.stream()
                .map(topic -> mapToResponseWithChildCount(topic, translations.get(topic.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get selectable topics for current user
     * Returns APPROVED topics + user's own PENDING topics with full hierarchical paths
     */
    @Transactional(readOnly = true)
    public List<com.my.challenger.dto.quiz.SelectableTopicResponse> getSelectableTopics(String languageCode) {
        log.debug("Getting selectable topics for current user");

        // Get current user ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long currentUserId = currentUser.getId();

        // Get selectable topics using repository method
        List<Topic> topics = topicRepository.findSelectableTopicsForUser(currentUserId);
        
        // Note: For selectable topics, we construct the full path name (e.g. "Geography > Geology").
        // Doing this with translation efficiently is tricky because we need translations for parents too.
        // For simplicity in this implementation, we will fetch translations for the *leaf* topics
        // and leave parents in English, OR we could fetch all system topic translations.
        // A better approach for enterprise would be to fetch all potentially needed translations.
        // Given the prompt's constraints, I'll translate the topic's own name, but for the path,
        // I might leave it or attempt a best effort if time permits.
        // To be safe and efficient, I'll just map the topic name itself. Path construction uses recursive parents.
        // I will skipping translating the full path for now to avoid massive N+1 queries or complex logic,
        // unless I preload all system translations.
        
        // Actually, let's try to translate at least the topic name.
        Map<Long, TopicTranslation> translations = getTranslationsMap(topics, languageCode);

        // Map to SelectableTopicResponse with full path
        return topics.stream()
                .map(topic -> mapToSelectableResponse(topic, currentUserId, translations.get(topic.getId())))
                .sorted((t1, t2) -> t1.getFullPath().compareToIgnoreCase(t2.getFullPath()))
                .collect(Collectors.toList());
    }

    /**
     * Move topic to a new parent
     * Updates parent reference; DB triggers will recalculate path and depth
     */
    @Transactional
    public TopicResponse moveTopic(Long topicId, Long newParentId) {
        log.info("Moving topic {} to new parent {}", topicId, newParentId);

        // Validate topic exists
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        // Validate newParent exists if not null
        Topic newParent = null;
        if (newParentId != null) {
            newParent = topicRepository.findById(newParentId)
                    .orElseThrow(() -> new ResourceNotFoundException("New parent topic not found with id: " + newParentId));

            // Check for circular reference: prevent moving topic to itself
            if (topicId.equals(newParentId)) {
                throw new IllegalArgumentException("Cannot move topic to itself");
            }

            // Check for circular reference: prevent moving topic to one of its descendants
            if (newParent.getPath() != null && topic.getPath() != null) {
                // If newParent's path contains this topic's id, it's a descendant
                String topicPathSegment = "/" + topicId + "/";
                if (newParent.getPath().contains(topicPathSegment)) {
                    throw new IllegalArgumentException("Cannot move topic to one of its descendants (circular reference)");
                }
            }
        }

        // Store old parent for logging
        Long oldParentId = topic.getParent() != null ? topic.getParent().getId() : null;

        // Update parent - DB triggers will recalculate path and depth for this topic and all descendants
        topic.setParent(newParent);
        Topic updatedTopic = topicRepository.save(topic);

        log.info("Topic {} moved from parent {} to parent {}", topicId, oldParentId, newParentId);

        return mapToResponse(updatedTopic, null);
    }

    /**
     * Map Topic entity to SelectableTopicResponse with full path
     */
    private com.my.challenger.dto.quiz.SelectableTopicResponse mapToSelectableResponse(Topic topic, Long currentUserId, TopicTranslation translation) {
        String name = topic.getName();
        if (translation != null) {
            name = translation.getName();
        }
        
        // Note: buildFullPath still uses English names for parents currently
        return com.my.challenger.dto.quiz.SelectableTopicResponse.builder()
                .id(topic.getId())
                .name(name)
                .fullPath(buildFullPath(topic)) // This might need refactoring for full localized path
                .depth(topic.getDepth())
                .validationStatus(topic.getValidationStatus())
                .isOwn(topic.getCreator() != null && topic.getCreator().getId().equals(currentUserId))
                .build();
    }

    /**
     * Build full hierarchical path from topic
     * Example: "Geography > Geology > Minerals"
     */
    private String buildFullPath(Topic topic) {
        if (topic.getParent() == null) {
            return topic.getName();
        }

        // Build path by traversing up the hierarchy
        StringBuilder pathBuilder = new StringBuilder();
        buildPathRecursive(topic, pathBuilder);
        return pathBuilder.toString();
    }

    private void buildPathRecursive(Topic topic, StringBuilder pathBuilder) {
        if (topic.getParent() != null) {
            buildPathRecursive(topic.getParent(), pathBuilder);
            pathBuilder.append(" > ");
        }
        pathBuilder.append(topic.getName());
    }
}
