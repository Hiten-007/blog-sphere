package org.hiten.blogspherebackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.dto.request.CreateTagRequest;
import org.hiten.blogspherebackend.dto.response.TagResponse;
import org.hiten.blogspherebackend.service.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Slf4j
public class TagController {

    private final TagService tagService;


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TagResponse> createTag(
            @Valid @RequestBody CreateTagRequest request) {

        log.info("Creating tag with name: '{}'", request.getName());

        TagResponse response = tagService.createTag(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public ResponseEntity<List<TagResponse>> getAllTags() {

        log.info("Fetching all tags");

        List<TagResponse> tags = tagService.getAllTags();

        log.info("Returned {} tags", tags.size());

        return ResponseEntity.ok(tags);
    }


    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> getTagById(@PathVariable Long id) {

        log.info("Fetching tag by ID: {}", id);

        TagResponse tag = tagService.getTagById(id);

        return ResponseEntity.ok(tag);
    }


    @GetMapping("/slug/{slug}")
    public ResponseEntity<TagResponse> getTagBySlug(@PathVariable String slug) {

        log.info("Fetching tag by slug: {}", slug);

        TagResponse tag = tagService.getTagBySlug(slug);

        return ResponseEntity.ok(tag);
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TagResponse> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody CreateTagRequest request) {

        log.info("Updating tag with ID: {}", id);

        TagResponse response = tagService.updateTag(id, request);

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {

        log.info("Deleting tag with ID: {}", id);

        tagService.deleteTag(id);

        return ResponseEntity.noContent().build();
    }
}