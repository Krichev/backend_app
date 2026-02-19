package com.my.challenger.web.controllers;

import com.my.challenger.dto.quiz.CreateTopicRequest;
import com.my.challenger.dto.quiz.MoveTopicRequest;
import com.my.challenger.dto.quiz.SelectableTopicResponse;
import com.my.challenger.dto.quiz.TopicResponse;
import com.my.challenger.dto.quiz.UpdateTopicRequest;
import com.my.challenger.service.impl.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "Topic management endpoints")
public class TopicController {

    private final TopicService topicService;

    @PostMapping
    @Operation(summary = "Create a new topic", description = "Create a new topic for quiz questions")
    public ResponseEntity<TopicResponse> createTopic(@Valid @RequestBody CreateTopicRequest request) {
        TopicResponse response = topicService.createTopic(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/roots")
    @Operation(summary = "Get root topics", description = "Get all root-level topics (no parent)")
    public ResponseEntity<List<TopicResponse>> getRootTopics(
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String languageCode) {
        List<TopicResponse> response = topicService.getRootTopics(languageCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/selectable")
    @Operation(summary = "Get selectable topics", description = "Get topics user can select (approved + own pending) with full hierarchical paths")
    public ResponseEntity<List<SelectableTopicResponse>> getSelectableTopics(
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String languageCode) {
        List<SelectableTopicResponse> response = topicService.getSelectableTopics(languageCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get topic by ID", description = "Retrieve a specific topic by its ID")
    public ResponseEntity<TopicResponse> getTopicById(
            @Parameter(description = "Topic ID") @PathVariable Long id,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String languageCode) {
        TopicResponse response = topicService.getTopicById(id, languageCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get topic children", description = "Get direct children of a topic")
    public ResponseEntity<List<TopicResponse>> getTopicChildren(
            @Parameter(description = "Parent topic ID") @PathVariable Long id,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String languageCode) {
        List<TopicResponse> response = topicService.getTopicChildren(id, languageCode);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/move")
    @Operation(summary = "Move topic", description = "Move topic to a new parent (or root if newParentId is null)")
    public ResponseEntity<TopicResponse> moveTopic(
            @Parameter(description = "Topic ID to move") @PathVariable Long id,
            @Valid @RequestBody MoveTopicRequest request) {
        TopicResponse response = topicService.moveTopic(id, request.getNewParentId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get topic by name", description = "Retrieve a topic by its name")
    public ResponseEntity<TopicResponse> getTopicByName(
            @Parameter(description = "Topic name") @PathVariable String name,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String languageCode) {
        TopicResponse response = topicService.getTopicByName(name, languageCode);
        return ResponseEntity.ok(response);
    }

//    @GetMapping
//    @Operation(summary = "Get all topics", description = "Retrieve all active topics with pagination")
//    public ResponseEntity<Page<TopicResponse>> getAllTopics(
//            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
//            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
//            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
//            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir) {
//
//        Sort sort = sortDir.equalsIgnoreCase("desc") ?
//                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
//        Pageable pageable = PageRequest.of(page, size, sort);
//
//        Page<TopicResponse> response = topicService.getTopics(pageable);
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/tree")
    @Operation(summary = "Get all topics (no pagination)", description = "Retrieve all active topics without pagination")
    public ResponseEntity<List<TopicResponse>> getAllTopicsNoPagination(
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String languageCode) {
        List<TopicResponse> response = topicService.getAllTopics(languageCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get topics by category", description = "Retrieve all topics in a specific category")
    public ResponseEntity<List<TopicResponse>> getTopicsByCategory(
            @Parameter(description = "Category name") @PathVariable String category,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String languageCode) {
        List<TopicResponse> response = topicService.getTopicsByCategory(category, languageCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all categories", description = "Retrieve all distinct topic categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = topicService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/search")
    @Operation(summary = "Search topics", description = "Search topics by name")
    public ResponseEntity<List<TopicResponse>> searchTopics(
            @Parameter(description = "Search term") @RequestParam String q,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String languageCode) {
        List<TopicResponse> response = topicService.searchTopics(q, languageCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular topics", description = "Get topics with the most questions")
    public ResponseEntity<List<TopicResponse>> getPopularTopics(
            @Parameter(description = "Number of topics to return") @RequestParam(defaultValue = "10") int limit,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String languageCode) {
        List<TopicResponse> response = topicService.getPopularTopics(limit, languageCode);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update topic", description = "Update an existing topic")
    public ResponseEntity<TopicResponse> updateTopic(
            @Parameter(description = "Topic ID") @PathVariable Long id,
            @Valid @RequestBody UpdateTopicRequest request) {
        TopicResponse response = topicService.updateTopic(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete topic", description = "Soft delete a topic")
    public ResponseEntity<Void> deleteTopic(
            @Parameter(description = "Topic ID") @PathVariable Long id) {
        topicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }
}
