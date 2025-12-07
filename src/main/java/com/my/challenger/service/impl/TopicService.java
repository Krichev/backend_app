package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.CreateTopicRequest;
import com.my.challenger.dto.quiz.TopicResponse;
import com.my.challenger.dto.quiz.TopicTreeResponse;
import com.my.challenger.dto.quiz.UpdateTopicRequest;
import com.my.challenger.dto.quiz.ValidateTopicRequest;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.ValidationStatus;
import com.my.challenger.entity.quiz.Topic;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopicService {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

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

        return mapToResponse(savedTopic);
    }

    /**
     * Get topic by ID
     */
    @Transactional(readOnly = true)
    public TopicResponse getTopicById(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));
        return mapToResponse(topic);
    }

    /**
     * Get topic by name
     */
    @Transactional(readOnly = true)
    public TopicResponse getTopicByName(String name) {
        Topic topic = topicRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with name: " + name));
        return mapToResponse(topic);
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
    public List<TopicResponse> getAllTopics() {
        return topicRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get topics with pagination
     */
    @Transactional(readOnly = true)
    public Page<TopicResponse> getTopics(Pageable pageable) {
        return topicRepository.findByIsActiveTrue(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get topics by category
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByCategory(String category) {
        return topicRepository.findByCategoryAndIsActiveTrue(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search topics by name
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> searchTopics(String searchTerm) {
        return topicRepository.searchByNameContaining(searchTerm).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
    public List<TopicResponse> getPopularTopics(int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        return topicRepository.findTopTopicsByQuestionCount(pageable).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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

        return mapToResponse(updatedTopic);
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
    private TopicResponse mapToResponse(Topic topic) {
        return TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .category(topic.getCategory())
                .description(topic.getDescription())
                .questionCount(topic.getQuestionCount())
                .isActive(topic.getIsActive())
                .creatorId(topic.getCreator() != null ? topic.getCreator().getId() : null)
                .parentId(topic.getParent() != null ? topic.getParent().getId() : null)
                .parentName(topic.getParent() != null ? topic.getParent().getName() : null)
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
    private TopicResponse mapToResponseWithChildCount(Topic topic) {
        int childCount = topicRepository.countByParentIdAndIsActiveTrue(topic.getId());

        return TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .category(topic.getCategory())
                .description(topic.getDescription())
                .questionCount(topic.getQuestionCount())
                .isActive(topic.getIsActive())
                .creatorId(topic.getCreator() != null ? topic.getCreator().getId() : null)
                .parentId(topic.getParent() != null ? topic.getParent().getId() : null)
                .parentName(topic.getParent() != null ? topic.getParent().getName() : null)
                .path(topic.getPath())
                .depth(topic.getDepth())
                .slug(topic.getSlug())
                .isSystemTopic(topic.getIsSystemTopic())
                .validationStatus(topic.getValidationStatus())
                .childCount(childCount)
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .validatedAt(topic.getValidatedAt())
                .build();
    }

    public Topic findOrCreateTopic(String topic, User creator) {
        return null;
    }

    /**
     * Get direct children of a topic
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicChildren(Long parentId) {
        log.info("Fetching children for topic: {}", parentId);

        // Verify parent topic exists
        if (!topicRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Topic not found with id: " + parentId);
        }

        return topicRepository.findByParentIdAndIsActiveTrue(parentId).stream()
                .sorted((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()))
                .map(this::mapToResponseWithChildCount)
                .collect(Collectors.toList());
    }

    /**
     * Get root topics (no parent)
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> getRootTopics() {
        log.info("Fetching root topics");

        return topicRepository.findByParentIsNullAndIsActiveTrue().stream()
                .sorted((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()))
                .map(this::mapToResponseWithChildCount)
                .collect(Collectors.toList());
    }

    /**
     * Get selectable topics for current user
     * Returns APPROVED topics + user's own PENDING topics with full hierarchical paths
     */
    @Transactional(readOnly = true)
    public List<com.my.challenger.dto.quiz.SelectableTopicResponse> getSelectableTopics() {
        log.debug("Getting selectable topics for current user");

        // Get current user ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long currentUserId = currentUser.getId();

        // Get selectable topics using repository method
        List<Topic> topics = topicRepository.findSelectableTopicsForUser(currentUserId);

        // Map to SelectableTopicResponse with full path
        return topics.stream()
                .map(topic -> mapToSelectableResponse(topic, currentUserId))
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

        return mapToResponse(updatedTopic);
    }

    /**
     * Map Topic entity to SelectableTopicResponse with full path
     */
    private com.my.challenger.dto.quiz.SelectableTopicResponse mapToSelectableResponse(Topic topic, Long currentUserId) {
        return com.my.challenger.dto.quiz.SelectableTopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .fullPath(buildFullPath(topic))
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