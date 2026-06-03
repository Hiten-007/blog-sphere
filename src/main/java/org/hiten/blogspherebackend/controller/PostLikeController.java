package org.hiten.blogspherebackend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.dto.response.LikeResponse;
import org.hiten.blogspherebackend.dto.response.PostResponse;
import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.service.PostLikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Slf4j
public class PostLikeController {

    private final PostLikeService postLikeService;

    /**
     * Like a post. Login is mandatory.
     * Returns 400 if the user has already liked the post (dislike first).
     */
    @PostMapping("/{postId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LikeResponse> likePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user) {

        log.info("userId={} liking postId={}", user.getId(), postId);
        LikeResponse response = postLikeService.likePost(postId, user.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a like (dislike). Only available if the user has already liked the post.
     * Returns 400 if the user has not liked the post.
     */
    @DeleteMapping("/{postId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LikeResponse> dislikePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user) {

        log.info("userId={} removing like from postId={}", user.getId(), postId);
        LikeResponse response = postLikeService.dislikePost(postId, user.getId());
        return ResponseEntity.ok(response);
    }

    // Get the like count and whether the current user has liked this post.
    @GetMapping("/{postId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LikeResponse> getLikeStatus(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user) {

        LikeResponse response = postLikeService.getLikeStatus(postId, user.getId());
        return ResponseEntity.ok(response);
    }

    // Get all posts liked by the currently authenticated user.
    @GetMapping("/liked")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PostResponse>> getLikedPosts(
            @AuthenticationPrincipal User user) {

        log.info("Fetching liked posts for userId={}", user.getId());
        List<PostResponse> posts = postLikeService.getLikedPostsByUser(user.getId());
        return ResponseEntity.ok(posts);
    }
}