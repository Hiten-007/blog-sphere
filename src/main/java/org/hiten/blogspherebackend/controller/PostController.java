package org.hiten.blogspherebackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.dto.request.CreatePostRequest;
import org.hiten.blogspherebackend.dto.request.UpdatePostRequest;
import org.hiten.blogspherebackend.dto.response.PostResponse;
import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.exception.BadRequestException;
import org.hiten.blogspherebackend.service.PostService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestPart("post") CreatePostRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) {

        log.info("Creating post with title: '{}' by user: {}", request.getTitle(), user.getEmail());

        PostResponse response = postService.createPost(request, file, user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Fetching posts - page: {}, size: {}, sortBy: {}, sortDir: {}",
                page, size, sortBy, sortDir);
        log.warn(">>> N+1 QUERY PROBLEM: Watch the SQL logs below! <<<");

        Page<PostResponse> posts = postService.getAllPosts(page, size, sortBy, sortDir);

        log.info("Returned {} posts (total: {})", posts.getNumberOfElements(), posts.getTotalElements());

        return ResponseEntity.ok(posts);
    }


    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id) {
        log.info("Fetching post by ID: {}", id);

        PostResponse post = postService.getPostById(id);

        // Increment view count asynchronously (fire and forget)
        try {
            postService.incrementViewCount(id);
        } catch (Exception e) {
            log.warn("Failed to increment view count for post {}: {}", id, e.getMessage());
        }

        return ResponseEntity.ok(post);
    }


    @GetMapping("/slug/{slug}")
    public ResponseEntity<PostResponse> getPostBySlug(@PathVariable String slug) {
        log.info("Fetching post by slug: {}", slug);

        PostResponse post = postService.getPostBySlug(slug);

        return ResponseEntity.ok(post);
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal User user) {

        log.info("Updating post {} by user: {}", id, user.getEmail());

        PostResponse response = postService.updatePost(id, request, user.getId());

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("Deleting post {} by user: {}", id, user.getEmail());

        postService.deletePost(id, user.getId());

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PostResponse>> getPostsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Fetching posts for user {} - page: {}, size: {}", userId, page, size);

        Page<PostResponse> posts = postService.getPostsByUser(userId, page, size);

        return ResponseEntity.ok(posts);
    }


    @GetMapping("/search")
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Searching posts with query: '{}' - page: {}, size: {}", query, page, size);

        Page<PostResponse> posts = postService.searchPosts(query, page, size);

        return ResponseEntity.ok(posts);
    }


    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<PostResponse>> getPostsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Fetching posts for category {} - page: {}, size: {}", categoryId, page, size);

        Page<PostResponse> posts = postService.getPostsByCategory(categoryId, page, size);

        return ResponseEntity.ok(posts);
    }


    @GetMapping("/tag/{tagId}")
    public ResponseEntity<List<PostResponse>> getPostsByTag(@PathVariable Long tagId) {
        log.info("Fetching posts for tag {}", tagId);

        List<PostResponse> posts = postService.getPostsByTag(tagId);

        return ResponseEntity.ok(posts);
    }


    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ResponseEntity<PostResponse> publishPost(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("Publishing post {} by user: {}", id, user.getEmail());

        PostResponse response = postService.publishPost(id, user.getId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ResponseEntity<PostResponse> uploadFeaturedImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {

        log.info("Uploading featured image for post {} by user: {}", id, user.getEmail());

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        PostResponse response = postService.uploadFeaturedImage(id, file, user.getId());

        return ResponseEntity.ok(response);
    }
}

