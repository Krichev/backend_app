package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.CreateTopicRequest;
import com.my.challenger.dto.quiz.TopicResponse;
import com.my.challenger.dto.quiz.UpdateTopicRequest;
import com.my.challenger.entity.User;
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
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .build();
    }

    public Topic findOrCreateTopic(String topic, User creator) {
        return null;
    }
}